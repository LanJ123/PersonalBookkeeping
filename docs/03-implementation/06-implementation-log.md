# 实现日志

本文件按时间追加第 4 阶段的代码产物、验证证据和遗留事项。构建/测试命令的完整结果后续归档到 `docs/04-testing/`。

## 2026-07-22：进入 I0/I1

### 输入

- 需求、UX、技术设计 v1.0 基线。
- Windows Android 环境必需项已通过；API 37 按 DEC-018 可选。

### 计划产物

- Gradle 9.5.0 Wrapper、AGP 9.3.0、Kotlin 2.3.21、KSP 2.3.9 版本目录。
- `:app` Compose 工程、隐私 Manifest 与手工 `AppContainer`。
- Money 纯 Kotlin 规则、Room schema v1、种子数据和创建交易最小垂直切片。
- 单元测试、构建、Lint 和验证报告。

### 状态

I0/I1 已完成并通过；I2 待开始。

### 首次构建兼容性记录

- AAR 元数据校验确认 Core 1.19.0 与 Lifecycle 2.11.0 要求 `compileSdk>=37`，不符合技术基线。
- 保持 `compileSdk=36`、`targetSdk=36` 不变，将 Core 固定为 1.17.0、Lifecycle 固定为 2.10.0；二者均为稳定版本，且覆盖本切片所需 API。
- 未采用忽略 AAR 元数据或强行升级 API 37 的方式绕过约束。
- Room 导出 schema v1 包含 6 张表、2 个派生视图；为 3 个非主键父引用补充外键索引，并同步参考 DDL。
- Lint 首轮指出缺少应用图标和强制竖屏；已增加自适应图标并取消方向锁定，手机仍保持竖屏优先布局但可适应系统旋转。

### 完成产物

- Gradle 9.5.0 Wrapper、官方分发 SHA-256、依赖校验元数据与固定版本目录。
- `:app` 单模块、无网络权限/禁用自动备份 Manifest、明暗主题与 v26/v33 自适应图标。
- Money 解析/格式化、交易形状校验、创建交易用例和手工 `AppContainer`。
- Room schema v1：6 张表、2 个派生视图、交易/预算触发器、默认账本/账户/分类种子。
- 可运行的支出/收入/转账最小编辑器、最近流水列表与 API 36 冒烟截图。

### 验证结果

- JVM：9/9 通过；API 28：3/3 通过；API 36：3/3 通过。
- `assembleDebug`、`assembleRelease`、`lintDebug`、androidTest 编译均成功；最终关键构建已使用依赖校验并离线复验。
- Lint 0 error；剩余 9 条均为已知版本更新提示，与 `compileSdk=36` 和固定工具版本基线一致。
- Release Manifest 不含 `INTERNET`；`allowBackup=false`，两代备份排除规则存在。
- Debug APK SHA-256：`6E3E1532A3DCED74713A7E42AA98126C068B3C2AA166485C801631BBE6BCE943`。
- 未签名 Release APK SHA-256：`5EF2BFB59969CFA8F4EC51148C561D4D3B1D5557149E0F79D934B41570A68639`。

完整明细见 [I1 测试报告](../04-testing/02-i1-test-report.md)。API 37 未执行且不构成失败；正式签名、完整四主导航和 I2 功能尚未开始。

## 2026-07-22：I2 与 I3

- I2 完成账户/分类管理、流水分页筛选、详情编辑、删除撤销与 Navigation 3 四主入口；JVM 16/16、API 28/36 各 8/8。详见 [I2 实现日志](08-i2-implementation-log.md)。
- I3 完成首页月概览、分类排行/日趋势/流水下钻、总预算和分类预算；JVM 18/18、API 28/36 各 11/11。详见 [I3 实现日志](10-i3-implementation-log.md)。
- 两个迭代均维持 Room schema v1、无网络权限和禁用系统自动备份；API 37 继续为可选前向兼容检查。

## 2026-07-23：I4

- 完成严格 `.pbk` v1 备份/恢复、恢复前复核与自动回滚快照、UTF-8 CSV 导出、全局金额隐私和可选系统应用锁。
- JVM 33/33、API 28/36 各 20/20；Debug/Release、Lint、依赖校验、权限与 schema 审计全部通过。
- API 36 真实 SAF 冒烟成功生成 `.pbk`，并归档页面截图、实际备份和 SHA-256。
- I4 保持 Room schema v1、无网络/存储权限并禁用系统自动备份；详见 [I4 实现日志](12-i4-implementation-log.md)和[I4 测试报告](../04-testing/08-i4-test-report.md)。
