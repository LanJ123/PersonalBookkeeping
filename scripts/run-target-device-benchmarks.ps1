[CmdletBinding()]
param(
    [string]$Serial = $env:ANDROID_SERIAL,
    [ValidateRange(1, 100)]
    [int]$Iterations = 30,
    [string]$OutputDirectory,
    [switch]$Canary,
    [switch]$AllowEmulatorTrend
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$targetPackage = "com.personalbookkeeping.app"
$providerUri = "content://$targetPackage.benchmark-data"
$uiTimeoutMs = 15000
$pollIntervalMs = 5
$repositoryRoot = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($Serial)) {
    throw "Specify -Serial or set ANDROID_SERIAL."
}
if ($Serial -notmatch "^[A-Za-z0-9._:-]+$") {
    throw "ANDROID_SERIAL contains unsupported characters: $Serial"
}
if ($Iterations -lt 30 -and -not $Canary) {
    throw "A physical-device release gate requires 30 iterations. Use -Canary for a shorter workflow check."
}
if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    throw "ANDROID_HOME is not set."
}

$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
    throw "adb was not found: $adb"
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputDirectory = Join-Path $repositoryRoot "benchmark\build\host-benchmark\$timestamp"
}
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$OutputDirectory = (Resolve-Path -LiteralPath $OutputDirectory).Path

$deviceLine = & $adb devices -l |
    Where-Object { $_ -match "^$([Regex]::Escape($Serial))\s+device(?:\s|$)" } |
    Select-Object -First 1
if ($null -eq $deviceLine) {
    throw "Target device is not online: $Serial"
}
$script:originalUserRotation = (
    & $adb -s $Serial shell wm user-rotation
).Trim()

$processInfo = [System.Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $adb
$processInfo.Arguments = "-s `"$Serial`" shell"
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardInput = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.CreateNoWindow = $true
$script:deviceShell = [System.Diagnostics.Process]::Start($processInfo)
$script:deviceCommandIndex = 0
$script:homeSignalPath = $null

function Invoke-DeviceShell {
    param(
        [Parameter(Mandatory)][string]$Command,
        [int]$TimeoutMs = 30000
    )

    if ($script:deviceShell.HasExited) {
        throw "Persistent adb shell exited with code $($script:deviceShell.ExitCode)."
    }

    $script:deviceCommandIndex++
    $marker = "__PBK_DONE_$($script:deviceCommandIndex)__"
    $script:deviceShell.StandardInput.WriteLine("$Command; echo $marker")
    $script:deviceShell.StandardInput.Flush()

    $lines = [System.Collections.Generic.List[string]]::new()
    while ($true) {
        $readTask = $script:deviceShell.StandardOutput.ReadLineAsync()
        if (-not $readTask.Wait($TimeoutMs)) {
            throw "Device command timed out after ${TimeoutMs}ms: $Command"
        }
        $line = $readTask.Result
        if ($null -eq $line) {
            throw "Persistent adb shell closed while running: $Command"
        }
        $markerIndex = $line.IndexOf($marker, [StringComparison]::Ordinal)
        if ($markerIndex -ge 0) {
            if ($markerIndex -gt 0) {
                $lines.Add($line.Substring(0, $markerIndex))
            }
            break
        }
        $lines.Add($line)
    }
    return $lines -join "`n"
}

function Reset-UiSignal {
    param([Parameter(Mandatory)][string]$Signal)

    $output = Invoke-DeviceShell(
        "content call --uri $providerUri --method reset-ui-signal --arg $Signal"
    )
    if ($output -notmatch "reset=true") {
        throw "Failed to reset UI signal '$Signal': $output"
    }
    if ($output -notmatch "generation=(\d+)") {
        throw "UI signal reset did not return a generation for '$Signal': $output"
    }
    $generation = [int]$Matches[1]
    if ($output -notmatch "signalPath=([^,\]}]+)") {
        throw "UI signal reset did not return a file path for '$Signal': $output"
    }
    return [pscustomobject]@{
        generation = $generation
        path = $Matches[1].Trim()
    }
}

function Wait-UiSignalFile {
    param(
        [Parameter(Mandatory)][string]$Signal,
        [Parameter(Mandatory)][string]$SignalPath,
        [Parameter(Mandatory)][int]$TargetPid,
        [Parameter(Mandatory)][int]$Generation,
        [int]$TimeoutMs = $uiTimeoutMs
    )

    $expected =
        "pid=$TargetPid signal=$Signal generation=$Generation marked=true"
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    while ($timer.ElapsedMilliseconds -lt $TimeoutMs) {
        $state = (
            Invoke-DeviceShell "cat '$SignalPath' 2>/dev/null"
        ).Trim()
        if ($state -eq $expected) {
            return $true
        }
        Start-Sleep -Milliseconds $pollIntervalMs
    }
    return $false
}

function Start-Target {
    Invoke-DeviceShell "am force-stop $targetPackage" | Out-Null
    Invoke-DeviceShell "input keyevent 224" | Out-Null
    Invoke-DeviceShell "wm dismiss-keyguard" | Out-Null
    $startOutput = Invoke-DeviceShell "am start -W -n $targetPackage/.MainActivity"
    if ($startOutput -notmatch "Status:\s+ok") {
        throw "Target launch failed: $startOutput"
    }
    $targetPidText = (Invoke-DeviceShell "pidof $targetPackage").Trim()
    if ($targetPidText -notmatch "^\d+$") {
        throw "Unable to resolve target process PID: $targetPidText"
    }
    $targetPid = [int]$targetPidText
    if (
        -not (
            Wait-UiSignalFile `
                -Signal "homeReady" `
                -SignalPath $script:homeSignalPath `
                -TargetPid $targetPid `
                -Generation 0
        )
    ) {
        throw "Home UI did not report ready within ${uiTimeoutMs}ms."
    }
    return $targetPid
}

function Measure-Action {
    param(
        [Parameter(Mandatory)][scriptblock]$Action,
        [Parameter(Mandatory)][string]$ReadySignal,
        [Parameter(Mandatory)][string]$SignalPath,
        [Parameter(Mandatory)][int]$TargetPid,
        [Parameter(Mandatory)][int]$Generation
    )

    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    & $Action
    if (
        -not (
            Wait-UiSignalFile `
                -Signal $ReadySignal `
                -SignalPath $SignalPath `
                -TargetPid $TargetPid `
                -Generation $Generation
        )
    ) {
        throw "UI signal '$ReadySignal' did not become ready within ${uiTimeoutMs}ms."
    }
    $timer.Stop()
    return [Math]::Round($timer.Elapsed.TotalMilliseconds, 3)
}

function Get-Percentile95 {
    param([Parameter(Mandatory)][double[]]$Samples)

    $sorted = @($Samples | Sort-Object)
    $index = [Math]::Ceiling($sorted.Count * 0.95) - 1
    return [Math]::Round([double]$sorted[$index], 3)
}

function Get-Median {
    param([Parameter(Mandatory)][double[]]$Samples)

    $sorted = @($Samples | Sort-Object)
    $middle = [Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 0) {
        return [Math]::Round(
            ([double]$sorted[$middle - 1] + [double]$sorted[$middle]) / 2.0,
            3
        )
    }
    return [Math]::Round([double]$sorted[$middle], 3)
}

function Write-Metric {
    param(
        [Parameter(Mandatory)][string]$FileName,
        [Parameter(Mandatory)][string]$Metric,
        [Parameter(Mandatory)][string]$Measurement,
        [Parameter(Mandatory)][double[]]$Samples,
        [AllowNull()][Nullable[double]]$ThresholdMs,
        [Parameter(Mandatory)][hashtable]$DeviceMetadata,
        [Parameter(Mandatory)][bool]$IsPhysicalDevice
    )

    $thresholdApplicable =
        $IsPhysicalDevice -and
        $Samples.Count -ge 30 -and
        $null -ne $ThresholdMs
    $p95 = Get-Percentile95 -Samples $Samples
    $passed = if ($thresholdApplicable) {
        $p95 -le [double]$ThresholdMs
    } else {
        $null
    }
    $payload = [ordered]@{
        metricVersion = 3
        metric = $Metric
        measurement = $Measurement
        driver = "host-persistent-adb-shell+benchmark-file-signal"
        deviceRole = if ($IsPhysicalDevice) {
            "target-physical-device"
        } else {
            "emulator-trend-only"
        }
        manufacturer = $DeviceMetadata.manufacturer
        model = $DeviceMetadata.model
        androidRelease = $DeviceMetadata.androidRelease
        sdkInt = $DeviceMetadata.sdkInt
        fingerprint = $DeviceMetadata.fingerprint
        transactionCountAtSeed = 10000
        iterations = $Samples.Count
        medianMs = Get-Median -Samples $Samples
        p95Ms = $p95
        thresholdMs = if ($null -ne $ThresholdMs) {
            [double]$ThresholdMs
        } else {
            $null
        }
        thresholdApplicable = $thresholdApplicable
        passed = $passed
        samplesMs = @($Samples | ForEach-Object { [Math]::Round($_, 3) })
    }
    $path = Join-Path $OutputDirectory $FileName
    [System.IO.File]::WriteAllText(
        $path,
        ($payload | ConvertTo-Json -Depth 6),
        [System.Text.UTF8Encoding]::new($false)
    )
    return [pscustomobject]$payload
}

function Invoke-SampledMetric {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][scriptblock]$Sample
    )

    $samples = [System.Collections.Generic.List[double]]::new()
    for ($iteration = 1; $iteration -le $Iterations; $iteration++) {
        $value = & $Sample
        $samples.Add([double]$value)
        Write-Host ("{0}: {1}/{2} = {3:N3} ms" -f $Name, $iteration, $Iterations, $value)
    }
    return [double[]]$samples.ToArray()
}

try {
    $shellReady = Invoke-DeviceShell "echo pbk-shell-ready"
    if ($shellReady -ne "pbk-shell-ready") {
        throw "Persistent adb shell did not initialize correctly: $shellReady"
    }
    $fingerprint = (Invoke-DeviceShell "getprop ro.build.fingerprint").Trim()
    $model = (Invoke-DeviceShell "getprop ro.product.model").Trim()
    $manufacturer = (Invoke-DeviceShell "getprop ro.product.manufacturer").Trim()
    $androidRelease = (Invoke-DeviceShell "getprop ro.build.version.release").Trim()
    $sdkInt = [int](Invoke-DeviceShell "getprop ro.build.version.sdk").Trim()
    $qemu = (Invoke-DeviceShell "getprop ro.kernel.qemu").Trim()
    $isEmulator =
        $qemu -eq "1" -or
        $fingerprint -match "generic|emulator" -or
        $model -match "Emulator|sdk_gphone"
    if ($isEmulator -and -not $AllowEmulatorTrend) {
        throw "The connected target is an emulator. Use -AllowEmulatorTrend only for non-gating trend runs."
    }
    $isPhysicalDevice = -not $isEmulator
    $deviceMetadata = @{
        manufacturer = $manufacturer
        model = $model
        androidRelease = $androidRelease
        sdkInt = $sdkInt
        fingerprint = $fingerprint
    }

    $thermal = Invoke-DeviceShell "dumpsys thermalservice"
    if ($thermal -match "Thermal Status:\s+([1-9]\d*)") {
        throw "Device thermal status is not 0. Cool the device before a release-gate run."
    }

    $sizeOutput = Invoke-DeviceShell "wm size"
    $sizeMatch = [Regex]::Matches(
        $sizeOutput,
        "(?:Physical|Override) size:\s*(\d+)x(\d+)"
    ) | Select-Object -Last 1
    if ($null -eq $sizeMatch) {
        throw "Unable to read device display size: $sizeOutput"
    }
    $width = [int]$sizeMatch.Groups[1].Value
    $height = [int]$sizeMatch.Groups[2].Value

    Invoke-DeviceShell "wm user-rotation lock 0" | Out-Null
    Invoke-DeviceShell "input keyevent 224" | Out-Null
    Invoke-DeviceShell "wm dismiss-keyguard" | Out-Null

    $seedOutput = Invoke-DeviceShell(
        "content call --uri $providerUri --method seed"
    )
    if ($seedOutput -notmatch "count=10000") {
        throw "Benchmark seed failed: $seedOutput"
    }
    $homeReset = Reset-UiSignal -Signal "homeReady"
    $script:homeSignalPath = $homeReset.path

    $coldStartSamples = Invoke-SampledMetric -Name "cold-start-ttfd" -Sample {
        Invoke-DeviceShell "am force-stop $targetPackage" | Out-Null
        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        $startOutput = Invoke-DeviceShell "am start -W -n $targetPackage/.MainActivity"
        if ($startOutput -notmatch "Status:\s+ok") {
            throw "Target launch failed: $startOutput"
        }
        $targetPidText = (Invoke-DeviceShell "pidof $targetPackage").Trim()
        if ($targetPidText -notmatch "^\d+$") {
            throw "Unable to resolve target process PID: $targetPidText"
        }
        if (
            -not (
                Wait-UiSignalFile `
                    -Signal "homeReady" `
                    -SignalPath $script:homeSignalPath `
                    -TargetPid ([int]$targetPidText) `
                    -Generation 0
            )
        ) {
            throw "Home UI did not report ready within ${uiTimeoutMs}ms."
        }
        $timer.Stop()
        [Math]::Round($timer.Elapsed.TotalMilliseconds, 3)
    }

    $ledgerX = [int]($width * 3 / 8)
    $bottomY = [int]($height * 13 / 14)
    $ledgerFirstSamples = Invoke-SampledMetric -Name "ledger-first-content-10k" -Sample {
        $targetPid = Start-Target
        $signalState = Reset-UiSignal -Signal "ledgerReady"
        Measure-Action `
            -ReadySignal "ledgerReady" `
            -SignalPath $signalState.path `
            -TargetPid $targetPid `
            -Generation $signalState.generation `
            -Action {
            Invoke-DeviceShell "input tap $ledgerX $bottomY" | Out-Null
        }
    }

    $ledgerScrollSamples = Invoke-SampledMetric -Name "ledger-scroll-10k" -Sample {
        $targetPid = Start-Target
        $signalState = Reset-UiSignal -Signal "ledgerReady"
        Invoke-DeviceShell "input tap $ledgerX $bottomY" | Out-Null
        if (
            -not (
                Wait-UiSignalFile `
                    -Signal "ledgerReady" `
                    -SignalPath $signalState.path `
                    -TargetPid $targetPid `
                    -Generation $signalState.generation
            )
        ) {
            throw "Ledger UI did not report ready within ${uiTimeoutMs}ms."
        }
        $centerX = [int]($width / 2)
        $fromY = [int]($height * 3 / 4)
        $toY = [int]($height / 4)
        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        1..3 | ForEach-Object {
            Invoke-DeviceShell(
                "input swipe $centerX $fromY $centerX $toY 250"
            ) | Out-Null
        }
        Start-Sleep -Milliseconds 250
        $timer.Stop()
        [Math]::Round($timer.Elapsed.TotalMilliseconds, 3)
    }

    $previousMonthX = [int]($width * 3 / 20)
    $previousMonthY = [int]($height / 4)
    $monthSamples = Invoke-SampledMetric -Name "previous-month-switch" -Sample {
        $targetPid = Start-Target
        $signalState = Reset-UiSignal -Signal "monthReady"
        Measure-Action `
            -ReadySignal "monthReady" `
            -SignalPath $signalState.path `
            -TargetPid $targetPid `
            -Generation $signalState.generation `
            -Action {
            Invoke-DeviceShell "input tap $previousMonthX $previousMonthY" | Out-Null
        }
    }

    $seedOutput = Invoke-DeviceShell(
        "content call --uri $providerUri --method seed"
    )
    if ($seedOutput -notmatch "count=10000") {
        throw "Benchmark reseed before save metric failed: $seedOutput"
    }
    $addX = [int]($width * 7 / 8)
    $addY = [int]($height * 23 / 28)
    $amountX = [int]($width / 2)
    $amountY = [int]($height * 9 / 28)
    $saveX = [int]($width / 2)
    $saveY = [int]($height * 31 / 40)
    $saveSamples = Invoke-SampledMetric -Name "save-feedback-10k" -Sample {
        $targetPid = Start-Target
        $editorState = Reset-UiSignal -Signal "editorReady"
        Invoke-DeviceShell "input tap $addX $addY" | Out-Null
        if (
            -not (
                Wait-UiSignalFile `
                    -Signal "editorReady" `
                    -SignalPath $editorState.path `
                    -TargetPid $targetPid `
                    -Generation $editorState.generation
            )
        ) {
            throw "Transaction editor did not report ready within ${uiTimeoutMs}ms."
        }
        Invoke-DeviceShell "input tap $amountX $amountY" | Out-Null
        Start-Sleep -Milliseconds 200
        Invoke-DeviceShell "input text 1.23" | Out-Null
        Invoke-DeviceShell "input keyevent 4" | Out-Null
        Start-Sleep -Milliseconds 200
        $saveState = Reset-UiSignal -Signal "saveReady"
        Measure-Action `
            -ReadySignal "saveReady" `
            -SignalPath $saveState.path `
            -TargetPid $targetPid `
            -Generation $saveState.generation `
            -Action {
            Invoke-DeviceShell "input tap $saveX $saveY" | Out-Null
        }
    }

    $results = @(
        Write-Metric `
            -FileName "i5-cold-start-ttfd.json" `
            -Metric "cold-start-ttfd" `
            -Measurement "force-stop-to-home-ui-signal-wall-clock" `
            -Samples $coldStartSamples `
            -ThresholdMs 2000.0 `
            -DeviceMetadata $deviceMetadata `
            -IsPhysicalDevice $isPhysicalDevice
        Write-Metric `
            -FileName "i5-ledger-first-content-10k.json" `
            -Metric "ledger-first-content-10k" `
            -Measurement "tap-to-first-ledger-card-ui-signal-wall-clock" `
            -Samples $ledgerFirstSamples `
            -ThresholdMs 1000.0 `
            -DeviceMetadata $deviceMetadata `
            -IsPhysicalDevice $isPhysicalDevice
        Write-Metric `
            -FileName "i5-ledger-scroll-10k.json" `
            -Metric "ledger-scroll-10k" `
            -Measurement "three-250ms-swipes-plus-250ms-settle-wall-clock" `
            -Samples $ledgerScrollSamples `
            -ThresholdMs $null `
            -DeviceMetadata $deviceMetadata `
            -IsPhysicalDevice $isPhysicalDevice
        Write-Metric `
            -FileName "i5-previous-month-switch.json" `
            -Metric "previous-month-switch" `
            -Measurement "tap-to-month-content-ui-signal-wall-clock" `
            -Samples $monthSamples `
            -ThresholdMs 1000.0 `
            -DeviceMetadata $deviceMetadata `
            -IsPhysicalDevice $isPhysicalDevice
        Write-Metric `
            -FileName "i5-save-feedback-10k.json" `
            -Metric "save-feedback-10k" `
            -Measurement "tap-to-save-success-ui-signal-wall-clock" `
            -Samples $saveSamples `
            -ThresholdMs 500.0 `
            -DeviceMetadata $deviceMetadata `
            -IsPhysicalDevice $isPhysicalDevice
    )

    $results |
        Select-Object metric, iterations, medianMs, p95Ms, thresholdMs, thresholdApplicable, passed |
        Format-Table -AutoSize
    Write-Output "Raw benchmark evidence: $OutputDirectory"

    $failed = @($results | Where-Object { $_.thresholdApplicable -and -not $_.passed })
    if ($failed.Count -gt 0) {
        throw "Target-device performance gate failed. Raw samples were preserved."
    }
}
finally {
    if ($null -ne $script:deviceShell -and -not $script:deviceShell.HasExited) {
        try {
            $script:deviceShell.StandardInput.Close()
        }
        catch {
            Write-Warning "Persistent adb shell could not be closed cleanly: $($_.Exception.Message)"
        }
        if (-not $script:deviceShell.WaitForExit(2000)) {
            $script:deviceShell.Kill()
        }
        $script:deviceShell.Dispose()
    }
    if ($script:originalUserRotation -match "^lock\s+([0-3])$") {
        & $adb -s $Serial shell wm user-rotation lock $Matches[1] | Out-Null
    } else {
        & $adb -s $Serial shell wm user-rotation free | Out-Null
    }
}
