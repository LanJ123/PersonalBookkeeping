# 测试策略

> 版本：v1.0（已基线化）  
> 目标：证明金额正确、数据可恢复、隐私约束真实存在，并控制回归风险

## 1. 测试层次

| 层次 | 运行位置 | 主要范围 |
|---|---|---|
| 纯 Kotlin 单元测试 | JVM | Money、日期、余额、预算、校验、CSV、备份 DTO、ViewModel |
| 数据库/Repository 集成 | Android instrumented | Room DAO、事务、触发器、Flow、Paging、迁移 |
| Compose UI 测试 | Android instrumented | 语义、输入校验、导航、空/错/锁定状态 |
| 端到端场景 | 模拟器/真机 | 记账→统计、备份→清空→恢复、系统文件选择器、应用锁 |
| 性能测试 | `:benchmark` + 真机 | 冷启动、1 万笔流水滚动、月份切换、保存延迟 |
| 发布审计 | 构建产物 | Manifest、依赖树、签名、R8、权限、APK 安装 |

## 2. 单元测试重点

- 金额：空、0、负数、1/2/3 位小数、最大值、格式化、分↔元往返。
- 交易：三种合法形状及所有缺失/冲突字段组合。
- 余额：收支、同币种转账、编辑前后差额、删除撤销、负余额、最大边界。
- 期间：月首/月末、闰年、跨年、时区改变、夏令时 zone、自然月边界。
- 预算：无预算、80%、100%、超支、分类与总预算独立。
- 名称：Unicode 规范化、空白、大小写、活动/停用唯一键。
- CSV：逗号、双引号、CR/LF、中文、空备注、BOM、稳定列顺序。
- 备份：schema 版本、摘要、引用、数量、限制、DTO 往返和旧格式 migrator。
- ViewModel：Loading/Content/Empty/Error、保存失败保留草稿、结果消费不重复。

金额和余额使用固定样例 + 可重复随机数据循环做性质验证，例如“同币种转账不改变总资产”“备份往返后实体与汇总一致”。不为此额外引入大型性质测试框架。

## 3. 数据库测试

- 每个 DAO 的正常查询、空结果、筛选组合、排序和分页。
- 非法 transaction/budget 触发器在 insert/update 均拒绝。
- 外键 `RESTRICT` 阻止删除已引用账户/分类。
- 活动名称唯一、停用后可复用名称。
- `withTransaction` 故障注入：转账、编辑和全量恢复只全成或全败。
- 余额 view 与独立 Kotlin 重算在随机账本上相等。
- 1→N 每条 migration 使用 `MigrationTestHelper`、Room 导出 schema 和旧版数据库 fixture；禁止破坏性迁移。

官方建议要求 Room schema 导出并以 `MigrationTestHelper` 验证数据保留：[Room migration 指南](https://developer.android.com/training/data-storage/room/migrating-db-versions)。

## 4. 备份/恢复测试矩阵

| 用例 | 预期 |
|---|---|
| 空流水账本往返 | 默认账户、分类、预算/偏好一致 |
| 1 万笔随机账本往返 | ID、字段、数量、余额、月汇总逐项一致 |
| 写出途中 I/O 失败 | 数据库不变，临时文件清理，结果可理解 |
| manifest/data 摘要不符 | 修改数据库前拒绝 |
| 更高 formatVersion | 显示不兼容，不尝试导入 |
| 悬空 account/category ID | 语义校验拒绝 |
| ZIP 路径穿越/重复条目/压缩炸弹 | 在资源限制内拒绝 |
| 恢复插入中故障 | Room 事务回滚；原账本可用 |
| 恢复成功后重启 | 恢复数据持续存在，临时日志清理 |

每个正式版本保存脱敏黄金 `.pbk` 与预期摘要。格式读取器必须能恢复所有承诺支持的旧 fixture。

## 5. UI 与无障碍测试

- 主流程：新增支出/收入/转账、编辑、删除/撤销、筛选、分类下钻、预算设置。
- 空状态、无结果、写入错误、恢复校验/进度/失败、无可用认证器。
- Compose 通过语义树定位，不以坐标或脆弱层级为主；语义同时服务 UI 测试与屏幕阅读器。
- 大字体 1.3 倍、320dp 宽、深浅主题、横竖屏打断后的草稿恢复。
- TalkBack 人工抽查首页、流水行、金额输入、预算和图表替代文本。

官方依据：[Compose 测试语义](https://developer.android.com/develop/ui/compose/testing/semantics)。

## 6. 设备矩阵

| 环境 | 用途 |
|---|---|
| API 28 模拟器 | 最低版本、Biometric/设备凭据分支、文件选择器 |
| API 36 模拟器 | target/compile 基线、日常自动化 |
| API 37 模拟器（可选） | Android 17 前向兼容；资源允许时执行，不作为发布目标或发布门槛 |
| 用户主力真机（型号待登记） | 最终安装、文件提供者、锁屏、性能与 ROM 行为 |
| 至少一台标准 Android 真机/参考环境 | 区分厂商 ROM 问题 |

主力机型号必须在实现阶段第一次设备测试前写入报告。

API 28、API 36 与目标真机是强制矩阵。API 37 仅提供额外前向兼容信号；因本机内存、镜像预览状态或其他资源限制未执行时，测试报告注明“可选项延期”即可，不判定失败。

## 7. 性能验证

- 使用 Release/profileable 构建和固定的 1 万笔数据集。
- Macrobenchmark 测冷启动、流水首屏、快速滚动和月份切换；普通保存延迟用 trace/基准场景测量。
- 以 NFR 的 P95 阈值作为发布门：冷启动 ≤2s、列表/月切换 ≤1s、保存反馈 ≤500ms。
- 真机运行并记录型号、Android、温度/电量、迭代数和原始 JSON；模拟器结果只做趋势，不做最终性能结论。

Android 官方说明 Macrobenchmark 用于启动与完整用户交互：[Benchmark 指南](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview)。

## 8. 自动化与发布门

每次提交运行：编译、单元测试、Android Lint、Debug 构建。进入主分支或发布候选运行 instrumented、Compose UI、迁移、备份黄金 fixture、Release 构建与 Manifest/依赖审计。性能测试在里程碑和发布候选真机运行。

发布必须满足：P0/P1 缺陷为 0；核心计算覆盖率 ≥80%；全部 migration 与备份 fixture 通过；Release 无 `INTERNET`、广告/分析 SDK；目标真机安装、备份恢复和应用锁通过；测试报告已归档。
