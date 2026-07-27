# I5 详细设计：硬化与发布候选

> 版本：v1.0
> 日期：2026-07-23
> 输入：需求、UX、技术设计 v1.0 与 I4 已通过代码基线

## 1. 范围与阶段边界

I5 覆盖 NFR-020～043 的发布前实现门，目标是形成可安装、可测量、可审计的候选版本：

- Release 启用 R8 代码优化与资源收缩，版本标识进入 `1.0.0-rc1`；
- 创建独立 `:benchmark` 模块、Release-like benchmark 变体和确定性 1 万笔数据集；
- 测量冷启动、流水首屏、快速滚动和月份切换，并保留原始 benchmark JSON/trace；
- 验证 320dp、1.3 倍字体、深浅主题、语义名称、非颜色状态表达和飞行模式；
- 对 API 28/36 重新执行全量回归、候选 APK 安装启动、Manifest、R8、依赖和 schema 审计；
- 归档发布清单、候选 APK 校验值、版本说明、用户手册和已知限制。

API 37 继续按 DEC-018 为可选前向兼容项。目标真机是最终性能、厂商文件提供者、系统锁屏和 ROM 行为的权威环境；当前未连接真机时，模拟器只提供趋势数据，不能把 NFR P95 判定为最终通过。

正式生产签名密钥不自动创建、不提交仓库，也不使用 Debug 密钥冒充正式身份。I5 可生成：

1. 未签名且已 R8 的 Release APK，供正式签名输入；
2. 使用本机 Debug 密钥签名的 `benchmark` 候选，仅用于安装、系统测试和性能采样。

## 2. 构建与发布候选

`release` 使用 `proguard-android-optimize.txt`、项目规则、`isMinifyEnabled=true`、`isShrinkResources=true` 和 `isDebuggable=false`。序列化、Room、Compose、Navigation、DataStore 与 Biometric 通过实际 Release 构建、安装和关键流程确认 R8 兼容；只在出现可复现缺失时增加最小 keep 规则。

`benchmark` 从 `release` 初始化，保持优化、收缩和非调试属性，只改用本机 Debug 签名以便安装。`src/benchmark/AndroidManifest.xml` 仅为该变体加入 `profileable` 和测试数据控制 Provider；正式 Release 不暴露测试入口。

候选版本固定：

```text
applicationId = com.personalbookkeeping.app
versionCode = 1
versionName = 1.0.0-rc1
```

正式发布前若已分发过同 applicationId/versionCode 的其他签名构建，必须登记并调整版本码；正式密钥一旦用于安装更新就必须离线长期保管。

## 3. 性能数据与测量

Benchmark-only Provider 同步创建一个脱敏确定性账本：1 个账本、2 个账户、支出/收入分类、偏好及 10,000 笔跨月流水。它只存在于 benchmark 变体，操作前全量替换 benchmark 数据库，不进入 Release Manifest。

Macrobenchmark 使用 AndroidX Benchmark 1.4.1 与 UI Automator 2.4.0：

- 冷启动：模拟器 10 次、目标真机 30 次 `StartupMode.COLD`，记录 TTID/TTFD 原始样本；
- 流水首屏：预置 10,000 笔，从点击“流水”到首条基准流水出现；模拟器 5 次、目标真机 30 次；
- 流水滚动：进入万条流水后执行三次固定滚动，保存 P95 趋势但不绑定当前 NFR 阈值；
- 月份切换：从点击“上月”到期间内容更新；模拟器 5 次、目标真机 30 次；
- 保存反馈：从点击“保存”到成功反馈出现；模拟器 5 次、目标真机 30 次；
- Baseline Profile：覆盖冷启动、首页、流水、统计和设置关键路径，生成后作为源码产物纳入 Release；
- 保存延迟：设备集成用例在 10,000 笔基础上测量普通新增事务，保留各次耗时。

API 36 模拟器无法稳定提供 FrameTimeline 样本且其 GfxInfo 输出与 AndroidX 采集器不兼容，因此帧级指标不作为本机门禁。模拟器结果明确标记 `emulator-trend-only`。目标真机结果标记 `target-physical-device`，记录型号、API 和构建指纹，并由主机脚本验证同设备、30 样本和 P95：冷启动 ≤2s、流水首屏/月切换 ≤1s、保存反馈 ≤500ms。低电量错误不在真机上抑制。

厂商真机若对 ProfileInstaller 广播返回异常结果、挂起 UiAutomation，或在目标 App 前台时冻结独立 benchmark 进程，则使用主机侧持久 ADB shell 驱动。benchmark-only Provider 在计时前预置数据并重置信号代次，Compose 目标布局完成后写入包含目标 PID、信号名和代次的小文件，主机以原生文件读取结束计时。该替代路径保持相同的动作边界、样本数、设备元数据和 P95 门槛；Release 不暴露 Provider，也不创建信号文件。

## 4. 无障碍与自适应

- 关键导航、浮动操作、返回、上/下月、筛选、编辑/删除、备份/恢复和锁定控件必须具备可访问名称；
- 流水收支方向和预算正常/接近/超出必须有文字，不只使用颜色；
- 图表提供等价文字语义，金额隐私开启时语义不得泄露金额；
- 320dp 宽与 1.3 倍字体下，新增流水、筛选、预算、备份和隐私核心操作必须可滚动到达，不发生控件重叠或截断；
- 深色、浅色、系统主题分别验证；TalkBack 人工抽查保存阅读顺序和操作结果；
- 系统旋转或进程重建不得造成数据库写入重复，编辑草稿的既有保存策略保持明确。

## 5. 兼容性、离线与安全

API 28 验证最低平台、旧 Biometric/凭据分支和 SAF；API 36 验证 target 行为、16 KB 页环境信号、系统文件选择器和候选安装。飞行模式下运行记账、统计、预算、备份/恢复和应用锁，合并 Release Manifest 必须继续满足：

- 无 `INTERNET`、网络状态、读写/管理外部存储、位置、通知、通讯录、短信、无障碍服务和悬浮窗权限；
- `allowBackup=false`，两代备份规则完整；
- 无广告、分析、崩溃上传或远程配置 SDK；
- Release 不含 benchmark Provider、`profileable` 测试覆盖或调试标志；
- 日志扫描不包含金额、备注、外部 URI、备份内容或认证数据。

## 6. 测试覆盖与质量

核心计算覆盖范围限定为 `common` 金额/名称、`domain` 规则与用例、`backup` 验证/归档、`export` CSV 和 `security` 锁策略；生成覆盖报告并以关键规则 ≥80% 为目标。Android UI、Room 生成代码、实体 DTO 和平台适配不混入该分母。

最终门禁包括 JVM、API 28/36、benchmark 编译/执行、Debug/Release/benchmark APK、Lint、R8 mapping、依赖校验、Manifest、Room schema、备份黄金样本、APK 安装与最小端到端场景。P0/P1 缺陷必须清零。

## 7. 退出规则

本机可完成项全部通过后，I5 可标记“工程硬化完成、外部发布门待验证”。只有以下外部输入闭环后，才可宣布 I5 完整退出并进入系统测试/正式发布：

1. 登记并连接目标真机，完成 P95、系统锁屏、SAF 和飞行模式复验；
2. 用户确定正式签名密钥策略，并在仓库外完成签名；
3. 对正式签名 APK 做安装/升级、备份恢复和 SHA-256 归档。
