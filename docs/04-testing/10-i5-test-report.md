# I5 测试报告

> 版本：v1.0
> 执行日期：2026-07-23
> 被测版本：`1.0.0-rc1`

## 1. 结论

I5 本地工程门禁通过：功能回归、核心覆盖率、构建、R8、Lint、Manifest、签名身份隔离、Baseline Profile 和模拟器性能采集链路均通过。没有发现 P0/P1 缺陷。

正式发布门禁尚未关闭，原因不是代码失败，而是缺少正式签名密钥和目标真机。当前结论为“工程硬化完成、外部发布门待验证”，不得把未签名 APK 或 Debug 签名 benchmark APK 当作正式安装包分发。

## 2. 环境与自动化结果

| 项目 | 结果 |
|---|---|
| JVM 单元测试 | 40/40，通过 |
| API 28 仪器测试 | 24/24，通过；0 failure/error/skip |
| API 36 仪器测试 | 24/24，通过；0 failure/error/skip |
| 核心行覆盖率 | 93.75%，门禁 ≥80%，通过 |
| 核心分支覆盖率 | 81.60%，门禁 ≥80%，通过 |
| Lint | 0 error、10 个非阻断依赖版本提示 |
| Release 构建 | APK、AAB、R8 mapping 均生成 |
| Benchmark | 万条滚动、月切换 2/2；冷启动 1/1，通过 |

API 37 按项目决策为可选前向兼容项，因本机内存不足未执行，不阻塞 I5。

## 3. 性能趋势

以下结果来自 1.5 GB 内存的 API 36 模拟器，只证明基准链路可重复执行，不能替代真机 P95：

| 指标 | 迭代 | 结果 |
|---|---:|---:|
| 10,000 条流水固定滚动 | 5 | 中位数 6964.417 ms |
| 切换上月 | 5 | 中位数 794.673 ms |
| 冷启动 TTID | 10 | min 1615.1 / median 1761.7 / max 1930.7 ms |
| 冷启动 TTFD | 10 | min 1615.1 / median 1792.4 / max 2630.9 ms |

AndroidX 报告明确警告模拟器结果不代表真实用户设备，因此 NFR-020～023 的最终 P95 判定保留为目标真机发布门。

## 4. 安全、隐私与产物审计

- Release 仅声明生物识别及 AndroidX 动态接收器所需权限；没有 `INTERNET`、广泛存储、位置、联系人等权限。
- `allowBackup=false`，两代备份规则均保留。
- Release 不含 `BenchmarkDataProvider`、`profileable` 或 debuggable 标志；benchmark 变体才包含测试 Provider/profileable。
- Release APK 当前未签名，`apksigner verify` 按预期失败；两个 benchmark APK 均由同一 Android Debug 证书使用 v2 签名，仅供测试。
- 依赖校验元数据、Room schema、R8 mapping 和 Baseline Profile 均已生成。

## 5. 用例处置

- 通过：TC-I5-001～007、009～013、016～018、020～024、026。
- 模拟器链路通过、真机阈值待验：TC-I5-005～008。
- 外部设备待验：TC-I5-014、015、019。
- 正式密钥和目标真机待验：TC-I5-025。

## 6. 证据

原始证据位于 [`evidence/i5`](evidence/i5/)：

- `api28-instrumented.xml`、`api36-instrumented.xml`
- `core-coverage.xml`、`core-coverage.csv`
- `lint-results-debug.xml`
- `i5-ledger-scroll-10k.json`、`i5-previous-month-switch.json`
- `startup-benchmark-data.json`、`startup-benchmark-summary.txt`

哈希和候选产物信息见[候选产物清单](../05-release/05-candidate-artifact-manifest.md)。
