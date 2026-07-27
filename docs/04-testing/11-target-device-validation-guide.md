# 目标真机验证执行指南

> 版本：v1.0
> 日期：2026-07-24
> 适用版本：`1.0.0-rc1` 及后续候选

## 1. 安全边界

真机性能测试会通过 benchmark-only Provider 清空 `com.personalbookkeeping.app` 的数据库并重建 10,000 条确定性流水。它只能在测试安装上执行。

如果手机中已存在需要保留的账本，必须先在 App 内创建 `.pbk` 并复制到其他安全位置。不得在没有备份时运行 benchmark，也不得把正式签名包与 Debug/benchmark 包混装。

## 2. 设备准备

1. 使用 Android 9/API 28 以上的目标主力手机；API 37 不是发布门。
2. 电量高于 50%，关闭省电模式、后台下载和系统更新，充电完成后拔线降温约 5 分钟。
3. 开启开发者选项和 USB 调试，接受电脑 RSA 授权。
4. 关闭所有模拟器，确保 `adb devices -l` 中只有目标手机。
5. 记录设备型号、Android/API、构建指纹、电池和温度状态。

```powershell
adb devices -l
$env:ANDROID_SERIAL = "设备序列号"
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint
adb shell dumpsys battery
adb shell dumpsys thermalservice
```

## 3. 自动化功能回归

真机不要直接执行整个 Compose/Espresso 测试集。部分厂商 Android 16 ROM 会在 Espresso 主消息循环同步器初始化阶段挂起。真机使用不初始化 Espresso 的 UiAutomator 兼容通道：

```powershell
$env:GRADLE_USER_HOME = "C:\Users\s5200\.gradle"
$env:ANDROID_SERIAL = "设备序列号"
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.personalbookkeeping.ui.physical.PhysicalDeviceComposeUiTest"
```

保持设备已解锁即可，不需要人工点击 App。脚本会通过 shell 显式启动 Activity，并覆盖导航、隐私设置即时刷新、记账保存、备份风险提示和 SAF 文件选择器。通过标准为 5/5、0 failure、0 error、0 skipped。

数据库与备份层另行执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.personalbookkeeping.data.local.AppDatabaseTest,com.personalbookkeeping.data.portability.PortabilityServiceTest"
```

通过标准为 11/11。完整 Compose 语义测试仍在 API 28/API 36 模拟器执行。保存 `app/build/outputs/androidTest-results/connected/debug/` 中的 XML。

## 4. 真机性能门

真机会自动使用每项 30 次采样；模拟器只运行短迭代并标记为 `emulator-trend-only`。先构建并安装 Release-like benchmark 主应用。该安装会清空或覆盖测试包的数据语义，不得用于保存真实账本。

```powershell
.\gradlew.bat :app:assembleBenchmark
adb install -r -t -g .\app\build\outputs\apk\benchmark\app-benchmark.apk
```

部分 vivo Android 16 设备的无线 ADB 流式安装会不返回，可改为两段式安装：

```powershell
adb push .\app\build\outputs\apk\benchmark\app-benchmark.apk /data/local/tmp/pbk-app-benchmark.apk
adb shell pm install -r -t -g /data/local/tmp/pbk-app-benchmark.apk
```

正式真机采样使用主机驱动脚本，避免厂商 ROM 冻结后台 instrumentation 或挂起 UiAutomation：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-target-device-benchmarks.ps1 `
  -Serial "$env:ANDROID_SERIAL" `
  -Iterations 30 `
  -OutputDirectory ".\docs\04-testing\evidence\target-device\YYYY-MM-DD-厂商-型号\performance"
```

脚本使用持久 ADB shell 和 benchmark-only 文件就绪信号。`content` Provider 只在计时前负责预置数据和重置信号，不进入交互计时区间。运行期间保持手机解锁；脚本会临时锁定竖屏并在结束或失败时恢复自动旋转，不修改屏幕超时时间。

采集指标：

| 指标 | 真机采样 | P95 门槛 |
|---|---:|---:|
| 冷启动 TTFD | 30 | ≤ 2000 ms |
| 10,000 条流水首屏 | 30 | ≤ 1000 ms |
| 月份切换 | 30 | ≤ 1000 ms |
| 保存至成功反馈 | 30 | ≤ 500 ms |

三次固定快速滚动另外保存 P95 趋势，但不作为当前 NFR 阈值。

原始输出位于传入的 `-OutputDirectory`，包括：

```text
i5-cold-start-ttfd.json
i5-ledger-first-content-10k.json
i5-ledger-scroll-10k.json
i5-previous-month-switch.json
i5-save-feedback-10k.json
```

运行主机侧门禁汇总：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-target-device-benchmarks.ps1 `
  -InputDirectory ".\docs\04-testing\evidence\target-device\YYYY-MM-DD-厂商-型号\performance"
```

汇总器会：

- 拒绝模拟器结果冒充真机；
- 要求四项门禁均至少 30 个样本；
- 验证启动与交互结果来自同一设备型号、API 和构建指纹；
- 重新按原始样本计算 P95；
- 输出 `target-device-verification.json`；
- 任一门槛不通过时返回非零退出码，同时保留报告。

只有开发脚本结构检查可以显式加入 `-AllowEmulatorTrend`，该参数不得用于发布结论。

## 5. 人工真机矩阵

- 新增支出、收入、转账；编辑、删除、撤销；余额和统计对账。
- 组合筛选、分类下钻、总预算和分类预算 80%/100% 边界。
- 浅色、深色、跟随系统以及 1.3 倍字体。
- TalkBack 阅读顺序、关键控件名称、图表等价文字和金额遮罩。
- 系统生物识别/设备凭据、取消认证、熄屏重入、最近任务预览。
- 系统 Files/厂商文件提供者中的 `.pbk` 创建、取消、损坏文件拒绝和往返恢复。
- 飞行模式下完成记账、查询、统计、预算、备份恢复和应用锁。
- 强制停止、旋转和设备重启后的数据持续性。

## 6. 正式签名候选

工程真机门通过后，在仓库外使用正式密钥签名 APK。验证首次安装、同签名覆盖安装、数据保留、备份恢复、证书摘要和 SHA-256。

Debug 与正式 APK 签名不同，不能直接覆盖。卸载任何已包含账本的安装前必须先导出 `.pbk`。

## 7. 证据归档

按设备保存到：

```text
docs/04-testing/evidence/target-device/YYYY-MM-DD-厂商-型号/
```

至少包含设备信息、自动化 XML、五项原始性能 JSON、30 次执行日志、`target-device-verification.json`、电池/温度、人工检查记录、截图、正式 APK 证书和 SHA-256。若设备支持标准 AndroidX Macrobenchmark，可额外归档 benchmarkData/trace，但不得以失败的厂商通道替代已校准的真机结果。

完成后更新 I5 测试报告、发布检查清单和候选产物清单，并以独立真机验证提交归档；正式签名 APK和密钥不得提交 Git。
