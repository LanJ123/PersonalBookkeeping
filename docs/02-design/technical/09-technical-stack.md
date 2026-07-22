# 技术栈与构建基线

> 版本：v1.0（已基线化）  
> 日期：2026-07-22  
> 输入基线：需求 v1.0、UX v1.0

## 1. 平台基线

| 项目 | 选择 | 理由 |
|---|---|---|
| 目标平台 | Android 手机，竖屏优先 | 与需求一致 |
| `minSdk` | 28（Android 9） | 覆盖 CR-002 恢复的下限；可直接使用 `java.time`、系统生物识别等能力 |
| `compileSdk` | 36 | Android SDK 官方稳定平台列表目前以 Android 16 / API 36 为稳定项 |
| `targetSdk` | 36 | 使用稳定目标行为；Android 17 / API 37 只作为可选兼容性检查，不在首版直接 target |
| 界面语言/币种 | 简体中文 / CNY | 与需求基线一致 |
| 屏幕 | 手机竖屏；320dp 及以上可用 | 平板不专项适配但布局不得崩坏 |

Android 17 作为非阻塞、可延期的测试项，不使用其专有 API；当前机器内存不足时无需运行。待 API 37 正式进入 SDK 稳定平台列表且依赖验证通过后再单独升级。

## 2. 构建工具锁定

| 工具 | 版本/策略 |
|---|---|
| Android Studio | Quail 2（2026.1.2）稳定版或兼容版本 |
| Android Gradle Plugin | 9.3.0 |
| Gradle Wrapper | 9.5.0；只使用项目自带 Wrapper，不依赖全局 Gradle |
| Kotlin | 2.3.21 |
| KSP | 2.3.9（KSP2） |
| Java toolchain | 17；Gradle 运行 JDK 可使用 Android Studio 自带 JBR |
| Compose BOM | `2026.06.00` 稳定 BOM |
| Room | 2.8.4 稳定版；不采用 Room 3 RC/预览版本 |
| Navigation 3 | 1.1.1 稳定版 |

版本全部写入 `gradle/libs.versions.toml` 或 Gradle Wrapper，禁止 `+`、`latest.release` 等动态版本。首次解析完成后启用 Gradle dependency verification 并提交校验元数据。

## 3. 应用技术选择

| 层面 | 选择 |
|---|---|
| 语言 | Kotlin，禁用新增 Java 业务代码 |
| UI | Jetpack Compose + Material 3；单 Activity |
| 导航 | Navigation 3，类型安全 `NavKey` |
| 状态 | ViewModel + `StateFlow` + `collectAsStateWithLifecycle`，单向数据流 |
| 并发 | Kotlin Coroutines / Flow；数据库写入与文件 I/O 不运行在主线程 |
| 数据库 | Room / SQLite；数据库是业务数据唯一事实源 |
| 列表 | Room PagingSource + Paging 3，仅用于完整流水列表 |
| 偏好 | DataStore Preferences 只保存设备本地安全/引导状态；可迁移业务偏好进入 Room |
| 序列化 | `kotlinx.serialization` JSON，用于 `.pbk` 逻辑备份契约 |
| 文件 | Android Storage Access Framework + `ContentResolver`；不申请广泛存储权限 |
| 应用锁 | AndroidX Biometric `BiometricPrompt` + 设备凭据回退 |
| 依赖注入 | 手工构造注入 `AppContainer`；不引入 Hilt/Dagger |
| 图表 | Compose Canvas/基础布局实现简单排行与折线，不引入图表 SDK |
| 日志 | 平台日志的薄封装，Release 移除 debug 日志并对敏感字段脱敏 |

## 4. Gradle 模块策略

MVP 使用两个模块：

- `:app`：全部产品代码与普通测试。
- `:benchmark`：Macrobenchmark 与 Baseline Profile，进入性能迭代时创建。

不为每个 feature 建 Gradle 模块。项目体量小，多模块会增加构建配置、导航契约和依赖管理成本。`app` 内通过严格包边界分层；只有当构建时间、团队规模或复用需求出现实际压力时再抽模块。

## 5. 明确不采用

- 不使用网络客户端、云 SDK、广告/分析/崩溃上报 SDK。
- 不使用跨平台框架；首版不采用 Kotlin Multiplatform。
- 不使用 SQLCipher：MVP 依赖 Android 沙箱与设备加密；应用锁是界面访问控制，不宣称数据库独立加密。
- 不使用 WorkManager：MVP 没有周期任务和通知。
- 不采用 alpha、beta、RC 依赖；Android 17 只做可选前向兼容检查。

## 6. 当前机器环境审计

2026-07-21 的复核结果：

- 已安装 Android Studio 2026.1.2，内置 JBR 21.0.10；另有 Zulu JDK 17、Zulu JDK 21.0.11 和 Git 2.55.0。系统 `JAVA_HOME` 指向 Zulu 17，系统 Path 中 JDK 17 排在 JDK 21 前。
- Android SDK 位于 `C:\Users\s5200\AppData\Local\Android\Sdk`；已安装精确 Platform 36、Build Tools 36.0.0、Platform Tools 37.0.0、Emulator 36.6.11 和 Command-Line Tools 22.0。
- 已安装 API 28、API 36 与 API 37.0 Google APIs x86_64 系统镜像；API 37 为 16 KB 页镜像。
- 已创建 `PBK_API_28`（Medium Phone）、`PBK_API_36`（Pixel 6）和 `PBK_API_37`（Pixel 6），AVD 配置及镜像引用有效；API 37 因本机内存限制按可选项延期。
- WHPX 加速检查通过；无需额外安装 HAXM 或模拟器 Hypervisor Driver。
- 系统 `ANDROID_HOME` 与 Android SDK 三条 Path 已持久化并生效。尚未创建 Gradle Wrapper 工程，符合当前阶段。

必需环境已按[验收报告](../../03-implementation/02-environment-verification.md)通过，可以初始化代码工程；API 37 不构成阶段门。全局 Gradle 无需安装，项目使用 Wrapper 9.5.0。

## 7. 官方依据

- [Android Gradle Plugin 版本、Gradle 兼容表及 Android Studio 兼容表](https://developer.android.com/build/releases/about-agp)
- [Android SDK 稳定平台发布记录](https://developer.android.com/tools/releases/platforms)
- [Compose BOM 官方说明](https://developer.android.com/develop/ui/compose/bom)
- [Room 稳定版本发布记录](https://developer.android.com/jetpack/androidx/releases/room)
- [Navigation 3 发布记录](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Android 架构建议](https://developer.android.com/topic/architecture/recommendations)
- [Android 构建中的 Java 版本](https://developer.android.com/build/jdks)
