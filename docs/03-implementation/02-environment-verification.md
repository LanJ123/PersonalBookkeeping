# Windows Android 开发环境验收报告

> 报告版本：v1.1  
> 验收日期：2026-07-22  
> 验收方式：只读命令检查、SDK/AVD 配置与镜像路径校验  
> 结论：必需环境安装与配置通过，可进入工程初始化；API 37 启动因内存限制按 DEC-018 延期，不构成阻塞

## 1. 验收摘要

| 检查项 | 实际结果 | 结论 |
|---|---|---|
| Android Studio | 2026.1.2，Build `AI-261.25134.95.2612.15822958` | 通过 |
| 项目 JDK | Zulu OpenJDK 17.0.19，`javac 17.0.19` | 通过 |
| Git | 2.55.0.windows.2 | 通过 |
| `JAVA_HOME` | 系统级 `C:\Program Files\Zulu\zulu-17\`，当前进程已生效 | 通过 |
| `ANDROID_HOME` | 系统级 `C:\Users\s5200\AppData\Local\Android\Sdk`，当前进程已生效 | 通过；路径属于当前 Windows 账户 |
| 持久化 SDK Path | 系统 Path 已包含 `platform-tools`、`emulator`、`cmdline-tools/latest/bin` | 通过 |
| 当前命令行 | `java`、`javac`、`git`、`adb`、`emulator`、`sdkmanager` 均解析到预期目录 | 通过 |
| SDK Platform | `platforms/android-36` | 通过 |
| Build Tools | 36.0.0 | 通过 |
| Platform Tools | ADB 37.0.0 | 通过 |
| Emulator | 36.6.11.0 | 通过 |
| Command-Line Tools | 22.0 | 通过；`sdkmanager` 的弃用提示不阻塞当前工作 |
| 模拟器加速 | `WHPX(10.0.28000) is installed and usable`，退出码 0 | 通过 |
| SDK 许可 | `android-sdk-license` 已存在 | 通过 |
| AVD 配置 | `PBK_API_28`、`PBK_API_36`、`PBK_API_37` 均可被工具识别 | 通过 |
| AVD 镜像引用 | 三套 `config.ini` 指向的系统镜像目录均存在 | 通过 |
| 正在连接的设备 | 复核时没有模拟器或真机正在运行，`adb devices -l` 为空 | 正常；不代表配置失败 |
| Gradle Wrapper/工程 | 尚无 `gradlew.bat`、`settings.gradle.kts` 等 | 符合当前阶段；工程初始化时创建 |

## 2. 已安装系统镜像

| API | Tag/ABI | Revision | 路径 |
|---|---|---|---|
| 28 | `google_apis/x86_64` | 11 | `system-images/android-28/google_apis/x86_64` |
| 36 | `google_apis/x86_64` | 7 | `system-images/android-36/google_apis/x86_64` |
| 37.0 | `google_apis,page_size_16kb/x86_64` | 6 | `system-images/android-37.0/google_apis_ps16k/x86_64` |

## 3. 已创建 AVD

| AVD | 硬件配置 | Android | 镜像 | 配置结论 |
|---|---|---|---|---|
| `PBK_API_28` | `medium_phone` | Android 9 / API 28 | Google APIs x86_64 | 有效 |
| `PBK_API_36` | `pixel_6` | Android 16 / API 36 | Google APIs x86_64 | 有效 |
| `PBK_API_37` | `pixel_6` | Android 17 / API 37.0 | Google APIs 16 KB x86_64 | 配置有效；启动可选并延期 |

Codex 执行环境会把 Java `user.home` 临时映射到沙箱目录，因此直接运行 `emulator -list-avds` 时可能看不到真实用户 AVD。将本次检查的 `ANDROID_AVD_HOME` 临时指向 `C:\Users\s5200\.android\avd` 后，Emulator 与 AVD Manager 均正确列出以上三台设备。这是审计沙箱差异，不是用户环境故障，不需要新增永久变量。

## 4. 运行验证范围

API 28、API 36 和目标真机属于强制测试环境。用户已确认除 API 37 外的环境均设置完成；正式测试报告仍应保存每次运行的设备与 ADB 结果。

`PBK_API_37` 因当前机器内存不足暂不启动。它只用于可选前向兼容检查，不阻塞工程初始化或发布。将来资源允许时，可单独启动并运行：

```powershell
adb shell getconf PAGE_SIZE
```

预期输出 `16384`。

若未来仍受本机资源限制，可以在更高内存机器或其他受控测试环境补做 API 37；不要求为此升级 `compileSdk` 或 `targetSdk`。

## 5. 工程初始化后的最终验证

项目骨架创建并生成 Gradle Wrapper 9.5.0 后执行：

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

验收要求：Gradle 为 9.5.0，Launcher/Daemon JVM 为 17，构建、单元测试和 Lint 全部通过。此项属于下一步工程初始化产物，不是当前环境安装缺失。
