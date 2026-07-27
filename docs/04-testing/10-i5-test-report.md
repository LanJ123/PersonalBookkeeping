# I5 测试报告

> 版本：v1.0
> 执行日期：2026-07-23
> 被测版本：`1.0.0-rc1`

## 1. 结论

I5 工程门禁及 vivo V2458A 真机性能门通过：功能回归、核心覆盖率、构建、R8、Lint、Manifest、签名身份隔离、Baseline Profile、真机功能自动化和四项 30 次/P95 性能指标均通过。没有发现 P0/P1 缺陷。

正式发布门禁尚未全部关闭，剩余外部项为正式签名、同签名升级和飞行模式人工复验。不得把未签名 APK 或 Debug 签名 benchmark APK 当作正式安装包分发。

2026-07-24 已补齐目标真机自动门：真机每项 30 次，覆盖冷启动 TTFD、万条流水首屏、月份切换和保存反馈，输出原始样本/P95/设备元数据，并由主机汇总器拒绝模拟器、样本不足、跨设备混用和超阈值。

2026-07-26 已接入 vivo V2458A（Android 16 / API 36）。数据库与备份自动化 11/11 通过，厂商兼容 Compose UI 黑盒自动化 5/5 通过，Debug 安装/启动、导航、隐私设置即时刷新、记账持久化及 SAF DocumentsUI 创建/取消均已验证。设备充电、拔线降温后完成五项各 30 次采样；四项发布阈值全部通过。TalkBack 服务开启后，设备持有人逐项点击首页、流水、金额输入、预算和趋势并确认均可正常阅读，TC-I5-014 通过。应用锁启用、后台/熄屏 30 秒重锁、取消后保持锁定、系统认证成功恢复以及最近任务隐私保护均由设备持有人确认正常，TC-I5-019 通过。该 ROM 会挂起 Compose/Espresso、UiAutomation 输入并冻结后台 benchmark 进程，正式性能采样因此使用持久 ADB shell 驱动和 benchmark-only 文件就绪信号。详见[目标真机测试报告](12-target-device-test-report.md)。

## 2. 环境与自动化结果

| 项目 | 结果 |
|---|---|
| JVM 单元测试 | 40/40，通过 |
| API 28 仪器测试 | 24/24，通过；0 failure/error/skip |
| API 36 仪器测试 | 24/24，通过；0 failure/error/skip |
| vivo 真机数据层 | 11/11，通过；0 failure/error/skip |
| vivo 真机 Compose UI 兼容通道 | 5/5，通过；0 failure/error/skip |
| vivo 真机性能门 | 四项各 30 次，P95 全部通过；滚动趋势 30 次已归档 |
| 核心行覆盖率 | 93.75%，门禁 ≥80%，通过 |
| 核心分支覆盖率 | 81.60%，门禁 ≥80%，通过 |
| Lint | 0 error、10 个非阻断依赖版本提示 |
| Release 构建 | APK、AAB、R8 mapping 均生成 |
| Benchmark | 模拟器趋势链路通过；vivo 真机四项 30 次/P95 门禁通过 |

API 37 按项目决策为可选前向兼容项，因本机内存不足未执行，不阻塞 I5。

## 3. 性能结果

vivo V2458A 正式真机结果：

| 指标 | 样本 | 中位数 | P95 | 门槛 | 结论 |
|---|---:|---:|---:|---:|---|
| 冷启动 TTFD | 30 | 390.973ms | 808.399ms | ≤2000ms | 通过 |
| 10,000 条流水首屏 | 30 | 152.427ms | 227.074ms | ≤1000ms | 通过 |
| 月份切换 | 30 | 68.469ms | 75.512ms | ≤1000ms | 通过 |
| 保存至成功反馈 | 30 | 81.902ms | 132.321ms | ≤500ms | 通过 |
| 三次固定滚动 | 30 | 1150.030ms | 1234.836ms | 趋势项 | 已归档 |

设备在开始时电量 52%、温度 37.1°C、Thermal Status 0，结束时电量 50%、温度 39.4°C、Thermal Status 1；全程未充电。独立汇总器结果为 `overallPassed=true`。

以下结果来自 1.5 GB 内存的 API 36 模拟器，只证明基准链路可重复执行，不能替代真机 P95：

| 指标 | 迭代 | 结果 |
|---|---:|---:|
| 10,000 条流水固定滚动 | 5 | 中位数 6964.417 ms |
| 切换上月 | 5 | 中位数 794.673 ms |
| 冷启动 TTID | 10 | min 1615.1 / median 1761.7 / max 1930.7 ms |
| 冷启动 TTFD | 10 | min 1615.1 / median 1792.4 / max 2630.9 ms |

AndroidX 报告明确警告模拟器结果不代表真实用户设备；NFR-020～023 已由上述 vivo 真机结果关闭。

## 4. 安全、隐私与产物审计

- Release 仅声明生物识别及 AndroidX 动态接收器所需权限；没有 `INTERNET`、广泛存储、位置、联系人等权限。
- `allowBackup=false`，两代备份规则均保留。
- Release 不含 `BenchmarkDataProvider`、`profileable` 或 debuggable 标志；benchmark 变体才包含测试 Provider/profileable。
- Release APK 当前未签名，`apksigner verify` 按预期失败；两个 benchmark APK 均由同一 Android Debug 证书使用 v2 签名，仅供测试。
- 依赖校验元数据、Room schema、R8 mapping 和 Baseline Profile 均已生成。

## 5. 用例处置

- 通过：TC-I5-001～007、009～014、016～024、026。
- 真机性能通过：TC-I5-005～008。
- 目标真机功能执行：TC-I5-014 的 TalkBack 人工矩阵，以及 TC-I5-019 的 Debug 安装/启动、导航、隐私状态刷新、记账持久化、应用锁认证和 SAF 创建/取消已通过；仅 TC-I5-015 飞行模式仍待验。
- 正式密钥与同签名升级待验：TC-I5-025。

## 6. 证据

原始证据位于 [`evidence/i5`](evidence/i5/)：

- `api28-instrumented.xml`、`api36-instrumented.xml`
- `core-coverage.xml`、`core-coverage.csv`
- `lint-results-debug.xml`
- `i5-ledger-scroll-10k.json`、`i5-previous-month-switch.json`
- `startup-benchmark-data.json`、`startup-benchmark-summary.txt`

vivo 真机原始样本、执行日志和独立汇总位于 [`evidence/target-device/2026-07-26-vivo-V2458A/performance`](evidence/target-device/2026-07-26-vivo-V2458A/performance/)。

哈希和候选产物信息见[候选产物清单](../05-release/05-candidate-artifact-manifest.md)。
