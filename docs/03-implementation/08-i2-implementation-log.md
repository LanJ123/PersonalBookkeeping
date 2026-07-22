# I2 实现日志

> 开始日期：2026-07-22  
> 完成日期：2026-07-22  
> 状态：已完成

## 输入与决策

- 用户批准进入 I2；范围按 [I2 详细设计](07-i2-detailed-design.md)执行。
- schema v1 保持不变；Navigation 3 固定 1.1.1，Paging 固定 3.5.0。
- API 28 与 API 36 为必测，API 37 继续可选。

## 实际产物

- `ui/navigation`：可保存的 Navigation 3 返回栈、首页/流水/统计/设置四主入口、全局“记一笔”。
- `database/dao`、`OfflineTransactionRepository`：Room PagingSource、同筛选条件日汇总、参数绑定、详情快照、编辑、删除及恢复。
- `OfflineManagementRepository`：名称 NFKC 规范化、活动名唯一、相邻排序、最后活动项保护、账户派生余额与停用历史保留。
- `ui/ledger`：搜索、类型/账户/分类/闭区间日期组合筛选、按日分组、空/错状态、详情和删除撤销 Snackbar。
- `ui/management`：账户和收入/支出分类的新增、编辑/重命名、排序、停用与已停用历史展示。
- [I2 测试用例](../04-testing/03-i2-test-cases.md)、[I2 测试报告](../04-testing/04-i2-test-report.md)和 API 36 截图。

## 实现与问题记录

1. 接入 `navigation3-runtime/ui:1.1.1`、`paging-runtime/compose:3.5.0`、`room-paging:2.8.4` 和 Kotlin serialization；依赖摘要写入 verification metadata。
2. Room KSP 编译证明新增 CTE 分页查询有效；导出 schema identity hash 与 I1 完全一致，未发生隐式迁移。
3. API 28 首轮测试暴露两个测试自身问题：相邻移动的预期写成直达首位；Paging 测试流在重组时被重复创建。修正测试契约后同设备 8/8 通过。
4. API 36 视觉冒烟发现设置入口误用 `fillMaxSize + weight` 造成超大按钮；改为全宽 64dp 标准入口并复查账户管理页面。
5. API 28/36 最终强制矩阵通过；API 37 未启动，符合 DEC-018。

## 验证结论

- JVM 16/16；API 28 8/8；API 36 8/8。
- Debug/Release 构建与 Lint 成功；Lint 0 error。
- Release/Debug Manifest 无 `android.permission.INTERNET`，系统自动备份关闭。
- Room schema v1 SHA-256 与 I1 相同；无数据结构变更。
- 无未关闭 P0/P1 缺陷。I2 退出，等待 I3 授权。
