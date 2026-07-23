# I4 测试报告

> 版本：v1.0
> 执行日期：2026-07-23
> 结论：通过；I4 退出条件满足

## 1. 范围与环境

I4 覆盖完整 `.pbk` 备份/恢复、CSV 导出、全局金额隐私、可选系统应用锁，以及 I1～I3 全量回归。平台保持 `minSdk=28`、`compileSdk=36`、`targetSdk=36`；API 28/36 AVD 均使用 x86_64 和 1.5 GB 内存。API 37 按 DEC-018 因当前机器内存不足未执行且不计失败。

构建使用 AGP 9.3.0、Gradle 9.5.0、JDK 17、Kotlin 2.3.21 和 KSP 2.3.9。I4 新增 kotlinx.serialization 1.11.0、DataStore 1.2.1、Biometric 1.1.0，并显式固定 Fragment 1.8.9；依赖校验元数据已更新并在最终构建中生效。

## 2. 结果汇总

| 范围 | 结果 | 结论 |
|---|---|---|
| JVM 单元测试 | 33 通过，0 失败，0 跳过 | 通过 |
| API 28 instrumented | 20 通过，0 失败，0 跳过 | 通过 |
| API 36 instrumented | 20 通过，0 失败，0 跳过 | 通过 |
| Debug APK | `assembleDebug` 成功 | 通过 |
| Release APK | `assembleRelease` 与 Lint Vital 成功；未签名 | 通过（开发迭代） |
| Android Lint | 0 error，10 条已知版本提示 | 通过 |
| API 36 人工冒烟 | 启动、设置入口、备份提示、真实 SAF 创建 `.pbk`、金额遮罩 | 通过 |
| P0/P1 缺陷 | 0 个未关闭 | 通过 |

## 3. 备份、恢复与恶意输入

- 黄金 `data.json` 经 DTO 解码、`.pbk` 写入和读取后，逻辑实体、时间、金额、计数和摘要保持一致。
- SHA-256/字节数损坏、未知字段、未来版本、重复/额外/目录/路径穿越/缺失 ZIP 条目均在生成恢复复核信息前拒绝。
- 归档压缩输入、解压数据和实体数量上限均有边界验证；读写两端统一执行 100 MiB 归档上限。
- 重复 ID、悬空引用、非法流水形状、非法预算作用域、非法日期/时区和重复活动名称由语义验证器阻断。
- Room 设备用例完成“备份—清空—恢复”往返，并验证恢复前 `noBackupFilesDir/pre-restore.pbk` 可读。
- 恢复事务中途注入故障后，事务回滚且原数据库保持不变；恢复复核页在确认前只显示时间、版本和实体计数。
- API 36 真实 SAF 冒烟产出的 [`.pbk` 文件](evidence/2026-07-23-i4-api36-smoke.pbk) 为 1,148 B，SHA-256 `A4A00D3ED3D18CF4395288B8EC7048AD4F5E86C5E12279EA859476966BF1D764`，只包含 `manifest.json` 和 `data.json`。

## 4. CSV、隐私与应用锁

- CSV 首部为 UTF-8 BOM，列顺序固定，行尾为 CRLF；中文、逗号、双引号、CR/LF 均按 RFC 4180 转义。
- 日期选择使用本地日期闭区间，数据库查询转换为左闭右开 epoch-day 范围；结果按发生时间升序，金额始终为正数两位小数，时间带偏移量。
- 金额隐私开启后，只读金额统一显示 `••••`；Compose 语义树和趋势图描述不出现真实金额。
- 应用锁冷启动不组合业务导航树；后台满 30 秒才重新锁定，计时使用单调时钟。
- 认证失败/取消保持锁定，认证成功才展示业务内容；无屏幕锁或生物识别能力时拒绝启用并显示可操作提示。
- 真实 `MainActivity` 的创建文档启动和取消回流均通过，覆盖 Fragment 与 Compose Activity Result 的集成路径。

## 5. schema、权限与依赖审计

- Room schema version 仍为 1，identity hash 为 `7b70326941632b2446b9030ab01d0a44`。
- schema 文件 SHA-256 为 `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504`，Git 无 schema diff。
- 合并 Release Manifest 不含 `android.permission.INTERNET`、读写外部存储或管理外部存储权限；Biometric/Fingerprint 权限属于系统认证组件的预期声明。
- `android:allowBackup="false"` 保持不变，`backup_rules.xml` 和 `data_extraction_rules.xml` 对 root、file、database、shared preferences 和 external 全部排除。
- 新增组件均有 SHA-256 依赖校验条目；最终 JVM、设备、Debug/Release 和 Lint 任务均在严格依赖校验配置下完成。

## 6. 构建与证据产物

| 产物 | 大小 | SHA-256 |
|---|---:|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 37,493,868 B | `0D96FEAC0C417D6ABAC7166BB96649C890C3BB5BDC280B07BB3240172F9E33BD` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 28,983,331 B | `C16637764A134E417B2E6B945ED08D61352E3039E7EFB8A2237C875BBB077FE7` |
| `app/schemas/com.personalbookkeeping.database.AppDatabase/1.json` | 21,730 B | `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504` |
| [API 36 首页](evidence/2026-07-23-i4-api36-home.png) | 128,630 B | `55D11CB3778D31B62B182EBB99C412F09C20E4FA6724C1F28D9530B508A9E456` |
| [API 36 设置](evidence/2026-07-23-i4-api36-settings.png) | 112,926 B | `DA5F4979EF936EA316645EB32A05B5B0205E472B01D3424B29EAFC3E7A89A3DB` |
| [API 36 数据与备份](evidence/2026-07-23-i4-api36-data.png) | 131,551 B | `C29A5265D4FCD4BED1F6EE5E2D88EB3C3428B36740798D2935982C090BDF3369` |
| [API 36 备份成功](evidence/2026-07-23-i4-api36-backup-success.png) | 156,366 B | `AE5ABA38DD8D9E0997AF2CF4C8A588D786BFC54FF45DC6D837FC27883F98CE3E` |
| [API 36 隐私设置](evidence/2026-07-23-i4-api36-privacy.png) | 130,843 B | `6986738CE90F6518BA87F369EA3C95391D140832A6636A4C9C56F0DF2FCB602D` |
| [API 36 金额遮罩](evidence/2026-07-23-i4-api36-hidden-amounts.png) | 126,501 B | `B288642A49371162AD9BAEA8E3E3CCEC4A1C34192545BBB732D04C722BDB12DF` |
| [API 36 实际备份](evidence/2026-07-23-i4-api36-smoke.pbk) | 1,148 B | `A4A00D3ED3D18CF4395288B8EC7048AD4F5E86C5E12279EA859476966BF1D764` |

`app/build/` 为可再生产物且不提交；源码、测试、黄金 fixture、Room schema、文档、截图和实际 `.pbk` 冒烟文件永久保存。正式签名 APK 在发布阶段生成。

## 7. 缺陷复测与退出结论

人工 SAF 冒烟发现并修复两个集成问题：旧 Fragment 传递版本造成请求码异常，以及通用 ZIP MIME 造成 `.pbk.zip` 文件名。两项均增加回归或真实产物证据。API 36 首轮最终设备测试的唯一失败来自 CSV 测试残留的旧 `single()` 断言，产品返回值正确；修正测试后 API 28/36 均重新全量通过。

Lint 的 10 条提示均为已评审的版本可用性信息，不含 error。API 37 是可选前向兼容项，未执行不影响结论。最终无未关闭 P0/P1 缺陷，I4 完成；进入 I5 需要用户另行确认。
