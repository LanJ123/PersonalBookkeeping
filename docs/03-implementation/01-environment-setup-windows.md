# Windows Android 开发环境准备指南

> 文档版本：v0.1  
> 审计日期：2026-07-21  
> 适用项目：PersonalBookkeeping  
> 平台基线：`minSdk=28`、`compileSdk=36`、`targetSdk=36`，API 37 仅做可选前向兼容测试

## 1. 当前结论

这台机器不需要重装 Android Studio，也不需要安装全局 Gradle。Android Studio、JDK 17、Android SDK Platform/工具/系统镜像、Git、三套 AVD、环境变量和模拟器加速均已完成。API 28/36 为必需环境；API 37 因内存限制可延期，不阻塞工程初始化。Gradle Wrapper 在工程初始化时生成。

| 项目 | 当前状态 | 需要的操作 |
|---|---|---|
| Windows | `10.0.28000.2525`，64 位环境 | 无需更换系统 |
| Android Studio | 已安装 `2026.1.2`，位于 `C:\Program Files\Android\Android Studio` | 保留并检查稳定更新 |
| Android Studio 运行时 | 内置 JBR 21.0.10 | 保留；不要设置 `STUDIO_JDK` 覆盖它 |
| 独立 Java | 已安装 Zulu JDK 17 与 Zulu JDK 21；系统 `JAVA_HOME` 指向 Zulu 17，系统 `Path` 中 17 排在 21 前 | 已完成；保留两个版本并重开终端使配置生效 |
| Git | 已安装 `2.55.0.windows.2` | 无需安装 |
| Android SDK | 已存在于 `C:\Users\s5200\AppData\Local\Android\Sdk` | 已完成 |
| SDK 工具 | Build Tools 36.0.0、Platform Tools 37.0.0、Emulator 36.6.11、Command-line Tools 已安装 | 在 SDK Manager 检查更新即可 |
| SDK 平台 | 已安装精确的 `platforms/android-36` | 已完成 |
| 系统镜像 | 已安装 API 28、API 36、API 37.0 Google APIs x86_64；API 37 为 16 KB 页镜像 | 已完成 |
| AVD | 已创建 `PBK_API_28`、`PBK_API_36`、`PBK_API_37`，配置与镜像路径有效 | API 28/36 必需；API 37 启动可选并延期 |
| 模拟器加速 | `WHPX(10.0.28000) is installed and usable` | 无需安装其他虚拟化驱动 |
| 环境变量 | 系统级 `JAVA_HOME`、`ANDROID_HOME` 和 Android SDK 三条 Path 均已生效 | 已完成 |
| Gradle | 没有全局 Gradle，也没有项目 Wrapper | 正常；不装全局 Gradle，建项目时提交 Gradle Wrapper 9.5.0 |

Android Studio 官方在 Windows 上要求启用 CPU 虚拟化；使用模拟器时最低建议 16 GB 内存，并提醒每个额外 AVD 可能占用约 6 GB。当前审计无法读取可用内存和磁盘空间，安装前需手工确认。参考：[Android Studio Windows 系统要求](https://developer.android.com/studio/install.html)。

## 2. 安装前检查

### 2.1 检查内存和磁盘

1. 按 `Ctrl + Shift + Esc` 打开任务管理器。
2. 进入“性能 > 内存”：建议 32 GB；最低应有 16 GB 才适合同时运行 Android Studio 和一个模拟器。
3. 打开“设置 > 系统 > 存储”，检查 C 盘。
4. 本项目需要三个 AVD，建议在安装前至少保留 35～40 GB 可用空间；如果 C 盘紧张，先决定是否把 AVD 数据放到其他本地 SSD。

不要把 AVD 放在同步盘、网络盘或机械移动硬盘上，否则启动和快照会明显变慢。

### 2.2 检查现有 Android Studio

1. 启动 `C:\Program Files\Android\Android Studio\bin\studio64.exe`。
2. 在欢迎页选择 `More Actions > Check for Updates`，或打开项目后选择 `Help > Check for Updates`。
3. 保持 Stable 更新通道；本项目不需要安装 Canary 版 IDE。
4. 不安装 Flutter、React Native、NDK、CMake、Firebase 或数据库服务器。

当前 Android Studio 已高于 Android 17 测试所需的最低 IDE 版本，因此无需为了 API 37 另装一套 IDE。[Android 17 运行环境说明](https://developer.android.com/about/versions/17/get)。

## 3. 准备 JDK 17

项目使用 AGP 9.3.0 和 Gradle 9.5.0。AGP 9.3.0 的官方兼容表要求 JDK 17，并以 Build Tools 36.0.0 为默认版本；当前机器的 JDK 21 不应代替项目约定的 Java toolchain 17。[AGP 9.3.0 兼容表](https://developer.android.com/build/releases/agp-9-3-0-release-notes)。

当前状态：Zulu JDK 17 已安装到 `C:\Program Files\Zulu\zulu-17\`，系统 `JAVA_HOME` 和系统 `Path` 已正确配置。本节保留为重装或在其他机器配置时的操作记录。

### 3.1 推荐：安装 Azul Zulu JDK 17 MSI

当前机器已经使用 Zulu JDK 21，因此继续安装同一发行商的 Zulu JDK 17 最清晰，两者可以并存。

1. 打开 [Azul Java 下载页](https://www.azul.com/downloads/?os=windows)。
2. 选择 Java 17（LTS）、Windows、x86 64-bit、JDK、非 JavaFX 版本。
3. 下载最新稳定补丁版本的 `.msi`，不要下载 JRE。
4. 双击 MSI。全机器安装需要管理员权限；也可以选择仅当前用户安装。
5. 在 `Custom Setup` 中启用：
   - `Add to PATH`
   - `Set JAVA_HOME variable`
6. 不需要启用 JavaSoft/Oracle 注册表兼容项。
7. 使用默认目录，通常为 `C:\Program Files\Zulu\zulu-17`。
8. 完成安装后关闭所有旧 PowerShell，再打开新窗口。

Azul 官方 MSI 支持并存安装、更新 `PATH` 和设置 `JAVA_HOME`。[Azul Zulu Windows 安装说明](https://docs.azul.com/core/install/windows)。

先直接验证新 JDK 文件：

```powershell
& 'C:\Program Files\Zulu\zulu-17\bin\java.exe' -version
```

再验证默认环境：

```powershell
$env:JAVA_HOME
java -version
Get-Command java
```

如果直接路径显示 17，但 `java -version` 仍显示 21，表示原 Zulu 21 在 `PATH` 中排得更前。按第 6 节把 `%JAVA_HOME%\bin` 移到 `C:\Program Files\Zulu\zulu-21\bin` 前面，不要卸载 JDK 21。

### 3.2 与 Android Studio 关联

无论使用哪种外部安装方式，项目创建后都需要让 Android Studio 使用它：

1. 进入 `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`。
2. 在 `Gradle JDK` 中选择 `JAVA_HOME`；若没有出现，选择 `Add JDK` 并指向 JDK 17 根目录。
3. 点击 `Apply`。

这一步只是选择已经安装的 JDK，不会通过 Android Studio 下载任何内容。

注意：

- Android Studio IDE 自身继续使用内置 JBR 21；不要设置 `STUDIO_JDK`。
- 不必卸载现有 Zulu JDK 21。
- 项目 Gradle 文件仍显式固定 Java toolchain 17，不能只依赖某台机器的默认 Java。
- 当前机器未检测到 `winget`，不建议为了安装 JDK 单独引入 Winget、Chocolatey 或 Scoop。
- Android 官方建议让命令行和 IDE 的 Gradle 构建使用一致的 JDK。[Android 构建中的 JDK 配置](https://developer.android.com/build/jdks)。

## 4. 补齐 Android SDK 组件

### 4.1 打开 SDK Manager

在 Android Studio 欢迎页选择 `More Actions > SDK Manager`；打开项目后也可选择 `Tools > SDK Manager`。确认顶部 `Android SDK Location` 为：

```text
C:\Users\s5200\AppData\Local\Android\Sdk
```

不要改到项目目录里。

### 4.2 SDK Platforms

1. 打开 `SDK Platforms` 页签。
2. 勾选右下角 `Show Package Details`。
3. 展开 Android 16（API 36）。
4. 勾选精确的 `Android SDK Platform 36`。当前已有的 `android-36.1` 不作为 `compileSdk=36` 所需精确平台的替代。
5. API 28 和 API 36 的系统镜像可以在此处选择，也可在创建 AVD 时按需下载。
6. 点击 `Apply`，查看下载清单并接受许可协议。

编译至少需要一个 Android SDK Platform，系统镜像则是运行模拟器所必需的。[SDK Manager 官方说明](https://developer.android.com/studio/intro/update.html)。

### 4.3 SDK Tools

在 `SDK Tools` 页签勾选 `Show Package Details`，确认下列组件：

- Android SDK Build-Tools `36.0.0`：已安装，保留。
- Android SDK Platform-Tools：已安装，更新到当前稳定版本。
- Android SDK Command-Line Tools (latest)：已安装，保留。
- Android Emulator：已安装，更新到当前稳定版本。
- Google USB Driver：只有使用 Pixel/Nexus 真机 USB 调试时安装；其他品牌使用厂商驱动。

不需要安装：

- NDK、CMake、LLDB：项目没有 C/C++ 原生代码。
- Android SDK Platform 37、Build-Tools 37、Sources for Android 37：API 37 只运行兼容性模拟器，应用仍以 API 36 编译和 target。
- Intel HAXM 或额外 Android Emulator Hypervisor Driver：当前 WHPX 已可用。

## 5. 创建三个 Android 虚拟设备

Android 官方通过 `Device Manager` 创建和管理 AVD；系统镜像可在创建过程中直接下载。[创建和管理 AVD](https://developer.android.com/studio/run/managing-avds)。

### 5.1 通用创建步骤

1. 在欢迎页选择 `More Actions > Virtual Device Manager`；打开项目后选择 `View > Tool Windows > Device Manager`。
2. 点击 `+`，选择 `Create Virtual Device`。
3. API 28 选择 Pixel 3 或 `Medium Phone` 等“Supported API Levels”包含 28 的硬件配置；Pixel 6 只支持 API 31+，不能用于 API 28。API 36/37 可以选择 Pixel 6。
4. 选择或下载对应系统镜像：本机 API 28、API 36 和 API 37 均使用已提供的 Google APIs Intel x86_64 Atom 镜像。
5. 点击 `Next`，按下表设置名称。
6. `Graphics` 使用 `Automatic` 或 `Hardware`，启动模式保留默认 Quick Boot，其他容量先用默认值。
7. 点击 `Finish`。
8. 首次分别启动三台 AVD，等待进入桌面后再关闭，以确认镜像和 WHPX 正常。

### 5.2 本项目 AVD 矩阵

| AVD 名称 | 系统镜像 | 用途 |
|---|---|---|
| `PBK_API_28` | Medium Phone；Android 9 / API 28 / Google APIs / Intel x86_64 Atom | 最低支持版本、文件选择器、生物识别和设备凭据分支 |
| `PBK_API_36` | Pixel 6；Android 16 / API 36 / Google APIs / Intel x86_64 Atom | 日常开发、`compileSdk/targetSdk` 基线和自动化测试 |
| `PBK_API_37` | Pixel 6；Android 17 / API 37 / Google APIs / Intel x86_64 Atom | 可选前向兼容测试；内存不足时延期，不改变 `targetSdk=36` |

## 6. 配置 Windows 用户环境变量

Android Studio 可以在没有环境变量时工作。当前系统 `ANDROID_HOME` 和三条 SDK Path 已持久化并通过命令解析复核。Android 官方建议使用 `ANDROID_HOME` 指向 SDK；`ANDROID_SDK_ROOT` 已弃用，不要新建。[Android 环境变量说明](https://developer.android.com/tools/variables)。

### 6.1 打开用户环境变量

1. 按 Windows 键，搜索“编辑账户的环境变量”。
2. Android SDK 位于当前账户的 `AppData`，因此它的 `ANDROID_HOME` 和 Path 使用上半部分“用户变量”。

系统变量也能生效，但适合安装在 `C:\Program Files`、供所有账户共用的软件。当前 Zulu JDK 17 已正确使用系统 `JAVA_HOME` 和系统 Path；无需重复创建用户级 `JAVA_HOME`。不要修改其他不熟悉的系统变量。

### 6.2 新建变量

当前系统变量已经存在，无需重复新建：

```text
变量名：ANDROID_HOME
变量值：C:\Users\s5200\AppData\Local\Android\Sdk
```

当前系统变量已经存在，无需重复新建：

```text
变量名：JAVA_HOME
变量值：C:\Program Files\Zulu\zulu-17\
```

不要填写到 `bin` 目录；`JAVA_HOME` 应指向 JDK 根目录。

### 6.3 编辑用户 Path

双击用户变量中的 `Path`，逐项新增：

```text
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\emulator
%ANDROID_HOME%\cmdline-tools\latest\bin
```

系统 Path 中已经依次存在 `C:\Program Files\Zulu\zulu-17\bin\` 和 `C:\Program Files\Zulu\zulu-21\bin\`，顺序正确，不必删除 JDK 21，也不必再向用户 Path 添加 Java。

保存全部对话框，然后关闭并重新打开 PowerShell、Codex 和 Android Studio，旧进程不会自动读取新变量。

### 6.4 验证命令

在新 PowerShell 中依次运行：

```powershell
java -version
git --version
adb version
sdkmanager --version
emulator -accel-check
emulator -list-avds
```

预期：

- `java -version` 显示 17；如果仍为 21，检查 Path 顺序。
- `adb`、`sdkmanager` 和 `emulator` 不再提示“无法识别”。
- `emulator -accel-check` 返回 `0` 并显示 WHPX 可用。
- `emulator -list-avds` 列出 `PBK_API_28`、`PBK_API_36`、`PBK_API_37`。

## 7. 准备一台真实手机

模拟器不能替代厂商 ROM、文件提供者、系统锁屏和真实性能验证。实现阶段第一次真机测试前准备一台 Android 9（API 28）或更高版本手机，并记录型号与 Android 版本。

### USB 调试

1. 手机进入“设置 > 关于手机”，连续点击“版本号/Build number”约 7 次，开启开发者选项。
2. 进入开发者选项，开启“USB 调试”。
3. 使用支持数据传输的 USB 线连接电脑。
4. 手机上接受电脑的 RSA 调试授权。
5. 在 PowerShell 运行 `adb devices`；状态应为 `device`，不能是 `unauthorized`。
6. 如果 Windows 未识别设备，Pixel/Nexus 安装 Google USB Driver，其他品牌从厂商支持站点安装 OEM ADB 驱动。[Windows OEM USB 驱动](https://developer.android.com/studio/run/oem-usb)。

Android 11 及以上也可以在 Android Studio 中选择 `Pair Devices Using Wi-Fi`，按二维码或配对码连接。[真机与无线调试](https://developer.android.com/studio/run/device)。

## 8. Gradle 与项目初始化

不要下载或安装全局 Gradle。实现阶段创建工程时应生成并提交：

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Wrapper 固定使用 Gradle 9.5.0，与 AGP 9.3.0 的官方兼容表一致。第一次同步会联网下载 Gradle 和 Maven 依赖；下载完成后再执行离线构建验证。

工程创建后验证：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

`gradlew --version` 中的 Gradle 应为 9.5.0，Launcher/Daemon JVM 应为 17。Android Studio 自动生成的 `local.properties` 应指向当前 SDK；该文件包含本机路径，不提交版本库。

## 9. 最终验收清单

- [ ] Android Studio 能正常启动，使用 Stable 通道。
- [x] JDK 17 已安装；项目创建后选为 Gradle JDK。
- [x] `ANDROID_HOME`、`JAVA_HOME` 和 Android SDK 三条 Path 已设置并生效。
- [x] Android SDK Platform 36 和 Build Tools 36.0.0 已安装。
- [x] Platform Tools、Command-Line Tools、Emulator 已安装。
- [x] `PBK_API_28`、`PBK_API_36` 配置有效且作为必需环境；用户确认其他环境已设置完成。
- [ ] `PBK_API_37` 配置有效，但因内存限制按可选项延期，不影响阶段通过。
- [x] `emulator -accel-check` 显示 WHPX 可用。
- [ ] `adb devices` 能识别至少一台模拟器；真机准备后也能识别真机。
- [ ] 项目使用 Gradle Wrapper 9.5.0，不依赖全局 Gradle。
- [ ] 首次 Debug 编译、单元测试和 Lint 通过。

完成前 8 项后即可进入 Android 工程初始化；最后一项在项目骨架建立后验收。
