# I1 测试报告

> 版本：v1.0  
> 执行日期：2026-07-22  
> 结论：通过；I0/I1 退出条件满足

## 1. 构建与环境

| 项目 | 实际值 |
|---|---|
| 应用 | `com.personalbookkeeping.app` / `0.1.0-dev` |
| 构建 | AGP 9.3.0、Gradle 9.5.0、JDK 17、KSP 2.3.9 |
| Android | `minSdk=28`、`compileSdk=36`、`targetSdk=36` |
| 设备 1 | `PBK_API_28`，Android 9 / API 28，x86_64 |
| 设备 2 | `PBK_API_36`，Android 16 / API 36，x86_64，测试时限制 2 GB 内存 |
| 可选设备 | API 37 未执行，依据 DEC-018 不计失败 |

## 2. 结果汇总

| 范围 | 结果 | 结论 |
|---|---|---|
| JVM 单元测试 | 9 通过，0 失败，0 错误，0 跳过 | 通过 |
| API 28 instrumented | 3 通过，0 失败，0 错误，0 跳过 | 通过 |
| API 36 instrumented | 3 通过，0 失败，0 错误，0 跳过 | 通过 |
| Debug APK | `assembleDebug` 成功 | 通过 |
| Release APK | `assembleRelease` + Lint Vital 成功；未签名 | 通过（开发迭代） |
| Android Lint | 0 error；9 条已知版本更新提示 | 通过 |
| androidTest 编译 | KSP/Kotlin/Java/资源打包成功 | 通过 |
| 离线/依赖校验复验 | `--offline` 下单测、构建、Lint 与 androidTest 编译成功 | 通过 |
| API 36 启动冒烟 | Debug 安装成功，Activity 冷启动成功且进程存活 | 通过 |

## 3. 数据与隐私审计

- Room 导出 schema v1：6 张表、2 个视图；identity hash `7b70326941632b2446b9030ab01d0a44`。
- schema 文件 SHA-256：`01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504`。
- 数据库设备测试证明支出、收入、转账派生余额一致，非法同账户转账被 SQLite 触发器拒绝。
- Release APK 权限表只有应用自身的动态接收器签名权限；没有 `android.permission.INTERNET`。
- 合并 Manifest 为 `allowBackup=false`，同时引用 Android 11 及以下与 Android 12+ 的全排除规则。
- Gradle Wrapper JAR SHA-256 与官方 9.5.0 值一致：`497C8C2A7E5031F6AA847F88104AA80A93532EC32EE17BDB8D1D2F67A194A9C7`；分发 ZIP 也配置官方 SHA-256。
- `gradle/verification-metadata.xml` 已生成，并用离线构建复验证明固定依赖可校验解析。

## 4. 产物

| 产物 | 大小 | SHA-256 |
|---|---:|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 30,836,284 bytes | `6E3E1532A3DCED74713A7E42AA98126C068B3C2AA166485C801631BBE6BCE943` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 23,459,343 bytes | `5EF2BFB59969CFA8F4EC51148C561D4D3B1D5557149E0F79D934B41570A68639` |
| `app/schemas/com.personalbookkeeping.database.AppDatabase/1.json` | 21,730 bytes | `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504` |
| [API 36 运行截图](evidence/2026-07-22-i1-api36.png) | 117,252 bytes | `9334918C18D64B0A0F42085966CA644E572811F6A93CC53ACDE5F31BE8038892` |

`app/build/` 为可再生且被 Git 忽略的构建目录；永久保存的是源码、Room schema、测试用例、本报告和截图。正式发布 APK 将在第 6 阶段签名并另存校验值。

## 5. 已知且接受的提示

Lint 剩余 9 条均为版本可用性提示：`OldTargetApi` 1 条、Gradle/AGP 版本提示 1 条、`GradleDependency` 5 条、Kotlin/Coroutines 新版提示 2 条。它们不是运行缺陷：

- API 37 与 Core/Lifecycle 新线会抬高 `compileSdk`，与已签署 API 36 基线冲突。
- Gradle 9.5.0、Kotlin 2.3.21 和 Coroutines 1.10.2 是当前技术基线的固定稳定版本。
- 后续升级必须单独评审依赖元数据、行为变化与 API 28/36 回归，不在本迭代顺手升级。

自适应图标 v26 fallback 与 v33 monochrome 分层符合平台资源规则；Lint 对 v26 文件的孤立误报已在 `app/lint.xml` 精确排除并写明原因，未全局关闭对应规则。

## 6. 结论与下一步

I0/I1 的源码、构建、金额规则、数据库约束、最小 UI 和强制设备矩阵均通过，未发现 P0/P1 缺陷。允许进入 I2：账户/分类管理、流水列表、组合筛选与编辑删除撤销。API 37 继续保持可选，不阻塞 I2。
