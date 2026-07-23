# I3 实现日志

> 开始日期：2026-07-22
> 完成日期：2026-07-23
> 状态：已完成

## 输入与决策

- 用户批准进入 I3；范围按 [I3 详细设计](09-i3-detailed-design.md)执行。
- Room schema v1 保持不变，月度数据由事实流水实时聚合。
- 趋势图使用 Compose 原生 Canvas，不增加第三方图表依赖。
- API 28 与 API 36 为必测；API 37 继续按 DEC-018 为可选项。

## 实际产物

- `domain/model/InsightsModels.kt`：自然月边界、月汇总、分类排行、日趋势、预算进度与整数阈值计算。
- `InsightsDao`、`OfflineInsightsRepository`：同周期汇总、分类/日期聚合、最近五笔、预算读取和新增/编辑/清除。
- `InsightsViewModel`：首页、统计和预算共享所选月份，统一加载与预算消息状态。
- `HomeScreen`：月支出主摘要、收入/结余、预算进度、最近流水和空状态。
- `StatisticsScreen`：分类金额/占比排行、可访问的日趋势和同月分类下钻。
- `BudgetsScreen`：总支出预算、活动支出分类预算、编辑/清除和非阻断状态提示。
- Navigation 3：默认入口改为首页；设置增加预算入口；统计下钻复用流水筛选。
- [I3 测试用例](../04-testing/05-i3-test-cases.md)、[I3 测试报告](../04-testing/06-i3-test-report.md)和两张 API 36 运行截图。

## 实现与问题记录

1. 预算阈值先判断 100%，再以 `used*5 >= limit*4` 判断 80%，避免浮点误差；权威金额始终为 `Long` 分。
2. 总预算使用 `TOTAL`，分类预算使用 `CATEGORY:<id>`；写入复用 schema v1 的唯一索引和触发器。
3. API 28 首轮 11 项测试中 1 项断言把 7000/8000 错写为正常状态；实际 87.5% 应为“接近预算”。仅修正测试期望，随后 API 28 全量 11/11 通过。
4. 最终合并门禁命令在工具 120 秒限制处超时；拆分为构建/JVM 和 Lint 后分别取得成功结果，不将工具超时计为测试失败。
5. API 36 首张截图抓到系统 Splash；确认前台 Activity 无崩溃并延长等待后，重新归档实际首页。
6. API 36 人工冒烟保存 ¥1,000 总预算并即时回流首页；从首页切到上月再进入统计，月份仍保持一致。
7. 2026-07-23 对最终源码重新执行设备矩阵：API 36 结果文件记录 11/11、退出码 0；API 28 命令记录 11/11、`BUILD SUCCESSFUL`。

## 验证结论

- JVM 18/18；API 28 11/11；API 36 11/11。
- Debug/Release 构建与 Lint 成功；Lint 0 error，10 条均为既有版本提示。
- Release Manifest 无 `android.permission.INTERNET`，`allowBackup=false`。
- Room schema v1 identity hash、文件 SHA-256 与 I1/I2 相同，无结构变更。
- API 36 安装、启动、首页、预算保存/回流、跨入口月份保留和设置入口人工冒烟通过。
- 无未关闭 P0/P1 缺陷。I3 退出，I4 需用户另行授权。
