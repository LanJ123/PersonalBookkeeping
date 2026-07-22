# I2 测试报告

> 版本：v1.0  
> 执行日期：2026-07-22  
> 结论：通过；I2 退出条件满足

## 1. 范围与环境

I2 覆盖账户/分类管理、流水分页与组合筛选、详情编辑、删除撤销和 I1 全量回归。平台保持 `minSdk=28`、`compileSdk=36`、`targetSdk=36`；API 28/36 AVD 均以 x86_64、1.5 GB 内存、无窗口模式执行。API 37 按 DEC-018 未执行。

新增核心依赖：Navigation 3 `1.1.1`、Paging `3.5.0`、Room Paging `2.8.4`；构建继续使用 AGP 9.3.0、Gradle 9.5.0、JDK 17、Kotlin 2.3.21 和 KSP 2.3.9。

## 2. 结果汇总

| 范围 | 结果 | 结论 |
|---|---|---|
| JVM 单元测试 | 16 通过，0 失败 | 通过 |
| API 28 instrumented | 8 通过，0 失败 | 通过 |
| API 36 instrumented | 8 通过，0 失败 | 通过 |
| Debug APK | `assembleDebug` 成功 | 通过 |
| Release APK | `assembleRelease` 与 Lint Vital 成功；未签名 | 通过（开发迭代） |
| Android Lint | 0 error，10 条已知版本提示 | 通过 |
| API 36 启动与视觉冒烟 | 安装/冷启动成功；进程存活；流水、设置和账户管理页复查 | 通过 |
| P0/P1 缺陷 | 0 个未关闭 | 通过 |

## 3. 功能与一致性证据

- 名称活动键执行 NFKC、去空白和 `Locale.ROOT` 小写；数据库唯一索引继续作为最终保护。
- 停用清空 `active_name_key`、不物理删除；新增交易选项排除停用项，历史详情仍通过外键显示原名称。
- 账户/分类相邻排序在 Room 事务中交换；账户和收入/支出分类均阻止停用最后一个活动项。
- 流水 PagingSource 使用参数绑定；备注查询转义 `%`、`_` 和反斜线；转账账户筛选匹配来源与目标端；日期为闭区间。
- 编辑保留交易 ID、发生时间与创建时间；测试证明支出改收入后余额只应用新影响一次。
- 删除先读取完整快照，撤销以原 ID 和时间字段恢复；往返后数量和余额一致。
- Compose 自动化验证筛选无结果和清除入口；API 36 截图验证最终流水页布局。

## 4. schema、隐私与依赖审计

- Room schema version 仍为 1，6 张表、2 个视图；identity hash `7b70326941632b2446b9030ab01d0a44`。
- schema SHA-256 仍为 `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504`，与 I1 完全相同。
- APK 权限表只包含应用自身的动态接收器签名权限；无 `android.permission.INTERNET`。
- 合并 Manifest 保持 `android:allowBackup="false"`，并引用两套全排除备份规则。
- I2 新依赖的 SHA-256 已写入 `gradle/verification-metadata.xml`，普通构建在严格依赖校验下成功。

## 5. 构建产物

| 产物 | SHA-256 |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | `D2BB1B8CBF6651EC017EC88A9DD0BA92935184868B303BDD0E9568676C48A978` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | `3002C9F4254ACB20086370875D5AA54DD8FD5F9F2F4ABE9F8C9DFFCB1F27FFD9` |
| `app/schemas/com.personalbookkeeping.database.AppDatabase/1.json` | `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504` |
| [API 36 运行截图](evidence/2026-07-22-i2-api36.png) | `55A59BF86205EB84FE3BA1C980E43EFC543B954946943ED63C1514DAB2C5E8F8` |

`app/build/` 为可再生产物且不提交；源码、Room schema、测试代码、文档和截图永久保存。正式签名 APK 仍在发布阶段生成。

## 6. 已知提示与结论

Lint 的 10 条提示均为已评审的版本可用性信息：5 条依赖更新、3 条工具/语言新版本、1 条 `OldTargetApi`、1 条 AGP 新版。提高到 API 37 或升级受其约束的 Core/Lifecycle 会改变已签署技术基线，本迭代不执行。

首轮 API 28 的两项失败均为测试契约/异步测试流问题，修正后 API 28 与 API 36 均全绿；API 36 视觉冒烟发现并修复设置入口尺寸问题。最终无未关闭 P0/P1 缺陷。I2 完成，I3 需用户另行确认后开始。
