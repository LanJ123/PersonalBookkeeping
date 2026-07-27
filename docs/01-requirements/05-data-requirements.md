# 数据需求

## 1. 核心实体

| 实体 | 关键字段 | 约束 |
|---|---|---|
| Ledger | id, name, currency, monthStartDay, createdAt | MVP 仅一个活动账本，currency=`CNY`，monthStartDay 固定为 1 |
| Account | id, ledgerId, name, type, openingBalanceMinor, includeInAssets, status, sortOrder | 金额单位为分；历史引用存在时只停用 |
| Category | id, ledgerId, kind, name, icon, color, status, sortOrder | kind 为 EXPENSE/INCOME；同 kind 活动名称唯一 |
| Transaction | id, ledgerId, type, amountMinor, categoryId, accountId, targetAccountId, occurredAt, note, createdAt, updatedAt | type 为 EXPENSE/INCOME/TRANSFER；金额为正整数 |
| Budget | id, ledgerId, period, categoryId?, amountMinor | categoryId 为空表示总预算；周期内同维度唯一 |
| Preference | key, value | 仅存非敏感设置；敏感密钥使用系统安全存储 |
| BackupManifest | formatVersion, appVersion, createdAt, counts, checksum | 位于备份包中，用于兼容与完整性校验 |

## 2. 交易规则

- EXPENSE：必须有支出分类和来源账户，`targetAccountId` 为空。
- INCOME：必须有收入分类和入账账户，`targetAccountId` 为空。
- TRANSFER：分类为空，来源与目标账户均存在且不同。
- `occurredAt` 保存明确时间点；展示与周期计算使用设备时区。跨时区规则在技术设计阶段固化。
- 数据库内部金额一律用 64 位整数“分”，不使用浮点数；界面与 CSV 负责格式转换。
- 新增、编辑、删除交易和恢复操作必须处于数据库事务中。

## 3. 派生值与一致性

- 账户余额、月度汇总、分类汇总和预算消耗均为流水派生值，不允许出现不可追溯的手工覆盖。
- 可缓存派生值以提升性能，但必须提供从事实流水重算并校验的能力。
- 同币种转账对总资产净影响为 0。
- `结余 = 统计周期收入 - 统计周期支出`，不含转账。
- 首页流水查询范围为设备当前自然月 `[月初, 下月月初)`，返回范围内全部未删除流水并按发生时间倒序。
- 分类构成按统计周期、交易类型和分类聚合；每项至少包含 `categoryId`、分类名、`COUNT(*)` 笔数和金额合计。支出/收入分别聚合，转账不进入构成。
- 同周期支出分类金额合计必须等于周期支出，收入分类金额合计必须等于周期收入；各分类笔数合计必须等于对应类型的流水笔数。

## 4. 删除与保留

- MVP 可使用确认删除 + 短时撤销；实现时可采用软删除或撤销日志，但导出和统计不得包含已删除记录。
- 停用的账户和分类必须保留历史可读性。
- 卸载应用将删除私有目录数据，因此首次形成有效数据后应提示用户创建外部备份，但不强制、不骚扰。

## 5. 备份格式要求

- 完整备份是有版本的单一文件或容器，至少含 manifest 和无损业务数据。
- 校验覆盖实际数据内容；恢复前验证格式版本、必填项、引用完整性和校验值。
- 备份不得依赖当前设备绝对路径。
- 技术设计阶段决定采用数据库快照还是结构化归档；无论选择何者，都必须通过跨版本恢复测试。

## 6. CSV 导出契约（v1 草案）

固定列：`type,amount,currency,category,account,target_account,occurred_at,note,created_at,updated_at`。

- 首行为英文稳定字段名，避免应用语言变化破坏脚本。
- UTF-8 编码，RFC 4180 风格转义；日期时间使用 ISO 8601。
- `amount` 为正的两位十进制文本，方向由 `type` 表达。
- 转账行 `category` 为空，`account` 为转出账户，`target_account` 为转入账户。
