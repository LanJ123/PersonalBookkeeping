# 编码规范

> 版本：v1.0  
> 生效日期：2026-07-22

## 1. 通用规则

- 新增业务代码使用 Kotlin；Java 17 toolchain，禁止依赖本机更高 JDK 的专有 API。
- 包名全小写；类型使用 `UpperCamelCase`，函数/属性使用 `lowerCamelCase`，常量使用 `UPPER_SNAKE_CASE`。
- 公共边界优先不可变数据；避免 `!!`、全局可变状态和无类型事件总线。
- 时间通过可注入 `Clock`，标识通过可注入 `IdGenerator`；测试不得依赖真实当前时间或随机 UUID。
- 业务日志不得包含金额、备注、备份 URI、数据库路径或认证信息。

## 2. 分层与依赖

- UI 只能依赖领域模型/用例接口，不直接引用 DAO、Room Entity、`ContentResolver` 或 DataStore。
- Domain 不依赖 Android UI；Money、交易校验和统计规则保持纯 Kotlin 可测。
- Database 负责持久化约束，Repository 负责映射，Use Case 负责业务形状和原子操作。
- 依赖由 `AppContainer` 显式创建；不得在业务类中隐式查找全局单例。

## 3. 金额、时间与文本

- 金额内部一律使用 `Long` 分；禁止 `Float`/`Double` 保存或计算金额。
- 输入先保留原始字符串，校验最多两位小数、正值和溢出，再转换为分。
- 交易同时保存 UTC 毫秒、IANA 时区和本地 epoch day；报表按本地日期查询。
- 名称和备注在领域边界规范化；备注空白转 `null`，最大 500 字符。

## 4. Compose

- Screen 只渲染不可变 `UiState` 并上送明确回调；副作用放在受控 effect 中。
- 可复用组件无导航和数据访问副作用；交互元素提供语义标签并满足 48dp 触控目标。
- 字符串、颜色、尺寸不得散落硬编码；用户可见文本进入资源文件。
- 每个页面覆盖加载、正常、空、校验失败和不可恢复错误等适用状态。

## 5. Room 与文件

- Entity 只用于数据库层；跨层必须映射。
- 所有写入在事务内完成；禁止 destructive migration。
- 每次 schema 变化提交 Room 导出 JSON、Migration 和迁移测试。
- 查询使用参数绑定；不得拼接用户输入 SQL。
- 外部文件按不可信输入处理：限制大小/条目、校验 schema/摘要/引用后才修改数据库。

## 6. 测试和提交门槛

- 新业务规则先写边界单测；数据库约束写 instrumented 测试；关键交互写 Compose 测试。
- Bug 修复必须增加能复现问题的回归测试。
- 提交前至少运行 `testDebugUnitTest`、`assembleDebug` 和 `lintDebug`；迭代结束补 `assembleRelease`。
- 不提交 `local.properties`、签名文件、密钥、生成 APK、IDE 用户配置和敏感日志。
