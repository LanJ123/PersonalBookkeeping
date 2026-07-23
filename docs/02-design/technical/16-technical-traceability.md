# 技术追踪矩阵

> 版本：v1.0（已基线化）  
> 基于需求 v1.0 与 UX v1.0；测试用例编号在测试设计落地时补充。

| 需求 | 主要实现模块 | 数据/接口 | 验证 |
|---|---|---|---|
| FR-001～003 | app、database/seed、security、settings UI | ledgers/defaults、DataStore、BiometricPrompt | 首启、Manifest、认证 UI 测试 |
| FR-010～016 | transaction UI、usecase、repository、Room | transactions、validate triggers、withTransaction | Money/交易单元测试、事务故障注入 |
| FR-020～024 | settings/accounts/categories、repository | accounts/categories、active_name_key、balance view | 唯一/停用/FK/余额重算测试 |
| FR-030～033 | ledger UI、PagingSource | period/account/category indexes | 组合查询、分页、空/错 UI 测试 |
| FR-040～045 | home/statistics/budget repository、ViewModel、Compose | 聚合 projections、budgets | TC-I3-001～012：汇总对账、月边界、下钻、预算阈值（已通过） |
| FR-050～055 | backup、export、SAF UI | `.pbk` v1、JSON Schema、CSV v1 | TC-I4-001～016：黄金 fixture、恶意输入、往返恢复、CSV（已通过） |
| NFR-001～005 | Manifest、security、backup validator | allowBackup/rules、限制、脱敏 | APK/Manifest/依赖/恶意文件审计 |
| NFR-010～013 | Room、migration、mutation coordinator | transactions、schema export、pre-restore | 原子性、迁移、崩溃/故障注入 |
| NFR-020～023 | benchmark、Paging、SQL 聚合 | 10k fixture、Macrobenchmark | Release 真机 P95 |
| NFR-030～034 | Compose theme/semantics、API 分支 | min28/target36、SAF、Biometric | API 28/36 强制矩阵、API 37 可选、飞行模式、大字体、TalkBack |
| NFR-040～043 | 分层、手工注入、Gradle 规则 | fakes、version catalog、Lint | 覆盖率、静态检查、可复现构建 |

## 架构约束检查

- UI 不直接调用 DAO、DataStore 或 ContentResolver。
- 金额不以 Float/Double 进入 domain/database/backup。
- 业务写入不绕过 use case/repository。
- Release Manifest 不得出现网络和广泛存储权限。
- 新 schema 版本必须同时更新 migration、Room export、DDL 参考、备份兼容评估和测试 fixture。
