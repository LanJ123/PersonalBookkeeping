# 构建与运行说明

> 版本：v1.1
> 更新日期：2026-07-23

## 1. 工程基线

- Windows 10/11、JDK 17、Android SDK Platform 36、Build Tools 36.0.0。
- 项目使用 `gradlew.bat` 和 Gradle 9.5.0，不要求安装全局 Gradle。
- `JAVA_HOME` 指向 JDK 17，`ANDROID_HOME` 指向本机 Android SDK。
- API 28/36 AVD 为强制兼容环境；API 37 AVD 可选且当前不启动。

## 2. 首次打开

1. 用 Android Studio 打开仓库根目录，不要选择 `app` 子目录。
2. 等待 Gradle Sync 完成；若提示 Gradle JDK，选择 JDK 17。
3. `local.properties` 由 Android Studio生成并保存 `sdk.dir`，该文件不提交 Git。
4. 运行配置选择 `app`，日常设备使用 `PBK_API_36`。

## 3. 命令行验收

在仓库根目录执行：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

I5 完整工程门与性能任务：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:verifyCoreCoverage :app:coreCoverageReport
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleRelease :app:bundleRelease :benchmark:assembleBenchmark
.\gradlew.bat :benchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.personalbookkeeping.benchmark.StartupBenchmark,com.personalbookkeeping.benchmark.LedgerBenchmark"
```

性能结果位于 `benchmark/build/outputs/connected_android_test_additional_output/`。模拟器结果只作趋势；正式 P95 必须在目标真机复跑。

安装到已启动模拟器：

```powershell
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n com.personalbookkeeping.app/com.personalbookkeeping.app.MainActivity
```

## 4. 兼容性运行

- 日常开发：`PBK_API_36`。
- 最低版本回归：`PBK_API_28`。
- API 37：资源允许时才启动 `PBK_API_37`，结果记录为补充验证，失败或未执行不阻塞构建。

在 Codex 沙箱内若 `emulator -list-avds` 未读取真实用户目录，可临时设置当前进程变量：

```powershell
$env:ANDROID_AVD_HOME = 'C:\Users\s5200\.android\avd'
emulator -list-avds
```

这不是系统变量缺失，不需要持久修改。

## 5. 常见问题

- `JAVA_HOME` 错误：确认 `java -version` 与 `javac -version` 都是 17。
- SDK 未找到：让 Android Studio 生成 `local.properties`，或确认 `ANDROID_HOME`。
- 依赖首次下载失败：确认代理/网络后重新 Sync；不要把版本改成动态版本。
- API 37 内存不足：关闭该 AVD，继续使用 API 28/36；无需改 `compileSdk` 或 `targetSdk`。
- Room schema 不一致：不得删除数据库绕过；更新 Migration、导出 schema 和测试。
