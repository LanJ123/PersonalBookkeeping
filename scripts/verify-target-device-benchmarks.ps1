[CmdletBinding()]
param(
    [string]$InputDirectory,
    [string]$OutputPath,
    [switch]$AllowEmulatorTrend
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($InputDirectory)) {
    $outputRoot = Join-Path $repositoryRoot "benchmark\build\outputs\connected_android_test_additional_output\benchmark\connected"
    $latestDeviceDirectory = Get-ChildItem -LiteralPath $outputRoot -Directory |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $latestDeviceDirectory) {
        throw "Benchmark output directory was not found: $outputRoot"
    }
    $InputDirectory = $latestDeviceDirectory.FullName
}

$InputDirectory = (Resolve-Path -LiteralPath $InputDirectory).Path
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $InputDirectory "target-device-verification.json"
}

function Read-JsonFile {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Required benchmark evidence is missing: $Path"
    }
    return Get-Content -LiteralPath $Path -Raw -Encoding utf8 | ConvertFrom-Json
}

function Get-P95 {
    param([Parameter(Mandatory)][double[]]$Samples)

    if ($Samples.Count -eq 0) {
        throw "Benchmark samples must not be empty."
    }
    $sorted = @($Samples | Sort-Object)
    $index = [Math]::Ceiling($sorted.Count * 0.95) - 1
    return [Math]::Round([double]$sorted[$index], 3)
}

function New-MetricResult {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][double[]]$Samples,
        [Parameter(Mandatory)][double]$ThresholdMs,
        [Parameter(Mandatory)][bool]$ThresholdApplicable
    )

    $p95 = Get-P95 -Samples $Samples
    return [pscustomobject][ordered]@{
        metric = $Name
        samples = $Samples.Count
        p95Ms = $p95
        thresholdMs = $ThresholdMs
        thresholdApplicable = $ThresholdApplicable
        passed = if ($ThresholdApplicable) { $p95 -le $ThresholdMs } else { $null }
    }
}

$coldStart = Read-JsonFile -Path (Join-Path $InputDirectory "i5-cold-start-ttfd.json")
$ledgerFirst = Read-JsonFile -Path (Join-Path $InputDirectory "i5-ledger-first-content-10k.json")
$monthSwitch = Read-JsonFile -Path (Join-Path $InputDirectory "i5-previous-month-switch.json")
$saveFeedback = Read-JsonFile -Path (Join-Path $InputDirectory "i5-save-feedback-10k.json")
$scrollPath = Join-Path $InputDirectory "i5-ledger-scroll-10k.json"
$scrollTrend = if (Test-Path -LiteralPath $scrollPath) {
    Read-JsonFile -Path $scrollPath
} else {
    $null
}

$roles = @(
    @($coldStart.deviceRole, $ledgerFirst.deviceRole, $monthSwitch.deviceRole, $saveFeedback.deviceRole) |
        Sort-Object -Unique
)
if ($roles.Count -ne 1) {
    throw "Custom benchmark device roles do not match: $($roles -join ', ')"
}
$deviceRole = [string]$roles[0]
$isPhysicalDevice = $deviceRole -eq "target-physical-device"
if (-not $isPhysicalDevice -and -not $AllowEmulatorTrend) {
    throw "Result role is $deviceRole. The release gate accepts only target-physical-device. Use -AllowEmulatorTrend only for development schema checks."
}

$customMetrics = @(
    @{ Payload = $coldStart; ExpectedName = "cold-start-ttfd" },
    @{ Payload = $ledgerFirst; ExpectedName = "ledger-first-content-10k" },
    @{ Payload = $monthSwitch; ExpectedName = "previous-month-switch" },
    @{ Payload = $saveFeedback; ExpectedName = "save-feedback-10k" }
)
foreach ($customMetric in $customMetrics) {
    if ($customMetric.Payload.metric -ne $customMetric.ExpectedName) {
        throw "Metric file mismatch. Expected $($customMetric.ExpectedName), found $($customMetric.Payload.metric)."
    }
    if (
        $customMetric.Payload.model -ne $ledgerFirst.model -or
        $customMetric.Payload.sdkInt -ne $ledgerFirst.sdkInt -or
        $customMetric.Payload.fingerprint -ne $ledgerFirst.fingerprint
    ) {
        throw "Custom benchmark device metadata does not match for $($customMetric.ExpectedName)."
    }
}

$metricInputs = @(
    @{ Name = "cold-start-ttfd"; Samples = @($coldStart.samplesMs | ForEach-Object { [double]$_ }); Threshold = 2000.0 },
    @{ Name = "ledger-first-content-10k"; Samples = @($ledgerFirst.samplesMs | ForEach-Object { [double]$_ }); Threshold = 1000.0 },
    @{ Name = "previous-month-switch"; Samples = @($monthSwitch.samplesMs | ForEach-Object { [double]$_ }); Threshold = 1000.0 },
    @{ Name = "save-feedback-10k"; Samples = @($saveFeedback.samplesMs | ForEach-Object { [double]$_ }); Threshold = 500.0 }
)

if ($isPhysicalDevice) {
    foreach ($metricInput in $metricInputs) {
        if ($metricInput.Samples.Count -lt 30) {
            throw "$($metricInput.Name) has $($metricInput.Samples.Count) samples; the release P95 gate requires at least 30."
        }
    }
}

$metricResults = @(
    foreach ($metricInput in $metricInputs) {
        New-MetricResult `
            -Name $metricInput.Name `
            -Samples $metricInput.Samples `
            -ThresholdMs $metricInput.Threshold `
            -ThresholdApplicable $isPhysicalDevice
    }
)

$report = [ordered]@{
    reportVersion = 1
    generatedAt = (Get-Date).ToString("o")
    deviceRole = $deviceRole
    device = [ordered]@{
        manufacturer = $ledgerFirst.manufacturer
        model = $ledgerFirst.model
        androidRelease = $ledgerFirst.androidRelease
        sdkInt = $ledgerFirst.sdkInt
        fingerprint = $ledgerFirst.fingerprint
    }
    sampleRequirement = if ($isPhysicalDevice) { 30 } else { $null }
    thresholdsApplied = $isPhysicalDevice
    overallPassed = if ($isPhysicalDevice) {
        -not ($metricResults | Where-Object { $_.passed -ne $true })
    } else {
        $null
    }
    metrics = $metricResults
    scrollTrend = if ($null -ne $scrollTrend) {
        [ordered]@{
            metric = "ledger-scroll-10k"
            samples = @($scrollTrend.samplesMs).Count
            p95Ms = Get-P95 -Samples @($scrollTrend.samplesMs | ForEach-Object { [double]$_ })
            thresholdApplicable = $false
        }
    } else {
        $null
    }
}

$outputDirectory = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    [System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
}
$json = $report | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText(
    $OutputPath,
    $json,
    [System.Text.UTF8Encoding]::new($false)
)

$metricResults |
    Select-Object metric, samples, p95Ms, thresholdMs, thresholdApplicable, passed |
    Format-Table -AutoSize
Write-Output "Report: $OutputPath"

if ($isPhysicalDevice -and -not $report.overallPassed) {
    throw "Target-device performance gate failed. Preserve the report and raw JSON files."
}
