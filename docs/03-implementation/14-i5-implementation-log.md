# I5 实现日志：硬化与发布候选

> 版本：v1.1
> 完成日期：2026-07-23
> 结论：本地工程硬化及 vivo 真机性能门完成；正式签名与人工真机项待外部条件

## 1. 实现内容

- 将版本标识更新为 `1.0.0-rc1`，Release 启用 R8 优化与资源收缩并生成 mapping。
- 新增独立 `:benchmark` 模块、Release-like benchmark 变体、10,000 条确定性测试数据 Provider。
- 新增冷启动 Macrobenchmark、万条流水滚动/月切换趋势基准和 Baseline Profile 生成链路。
- 将 1,079 条 Baseline Profile 规则纳入 Release；APK 已包含 `baseline.prof` 与 `baseline.profm`。
- 新增 JaCoCo 核心覆盖率任务，门禁为行覆盖率和分支覆盖率均不低于 80%。
- 完成 320dp、1.3 倍字体、可滚动设置页、装饰图标语义、主题切换和关键状态文本硬化。
- 将应用锁状态存储抽象为接口，补齐锁状态机、备份校验和主题持久化测试。
- SAF 设备测试改为等待系统 DocumentsUI 真正出现后再模拟取消，兼容低内存模拟器的冷启动。

## 2. 关键问题与处理

| 问题 | 根因 | 处理 |
|---|---|---|
| API 36 无法产生稳定 FrameTimeline/GfxInfo 数据 | 该模拟器返回 0 帧样本，GfxInfo 输出格式也与采集器不兼容 | 滚动和月切换改为固定数据、固定动作、固定迭代的端到端墙钟趋势；最终 P95 仍留给真机 |
| Baseline Profile 首次 Release 构建失败 | 规则文件带 UTF-8 BOM | 去除 BOM 后重新构建并验证 APK 内 dexopt 资产 |
| SAF 取消测试偶发超时 | 低内存 API 36 上 DocumentsUI 冷启动超过 15 秒 | 通过 UI Automator 最长等待 30 秒，确认系统选择器出现后再返回 |

## 3. 验证摘要

- JVM：40/40。
- API 28：24/24；API 36：24/24。
- 核心覆盖率：行 93.75%，分支 81.60%。
- Lint：0 error、10 warning；warning 均为非阻断的依赖版本提示。
- 构建：Debug APK、Release APK/AAB、benchmark APK、AndroidTest APK 全部成功。
- 性能链路：10,000 条滚动、月份切换和 10 次冷启动均执行成功并保存原始 JSON。
- Release Manifest：无网络和广泛存储权限，`allowBackup=false`，无 benchmark Provider/profileable。

## 4. 阶段边界

当前仓库只生成未签名 Release 输入以及 Debug 密钥签名的 benchmark/测试 APK。Debug 密钥不作为正式身份。以下事项必须在仓库外完成后才能正式发布：

1. 准备和离线保管正式签名密钥，签名并验证升级安装。
2. 在目标真机执行飞行模式和剩余厂商 ROM 复验；性能 P95、SAF、TalkBack 与系统应用锁已在 vivo V2458A 完成。
3. 对正式签名 APK 归档 SHA-256、安装/升级与备份恢复结果。

## 5. 2026-07-24 真机门禁补丁

- 真机指标统一使用 30 个样本并输出按最近秩计算的 P95；模拟器继续使用短迭代。
- 新增万条流水首屏和保存反馈基准，保留月份切换、滚动趋势与 AndroidX 冷启动。
- 输出动态设备角色、型号、API、构建指纹、阈值适用性和通过状态。
- 新增 `scripts/verify-target-device-benchmarks.ps1`，重新计算四项 P95、拒绝模拟器和混用设备证据，并对超阈值返回失败。
- 交易编辑器暴露仅用于自动化定位的 Compose resource test tag，不改变 TalkBack 文案。
- API 36 快速验证中，流水首屏与保存反馈各 5 次均执行成功；模拟器结果只验证链路，不关闭真机门。

## 6. 2026-07-26 vivo 真机性能闭环

- vivo Android 16 对 ProfileInstaller 广播返回异常结果，UiAutomator 节点查询/输入注入可能不返回，且独立 benchmark 进程在目标 App 前台后会被 `fast_freezer` 冻结。
- 新增 `scripts/run-target-device-benchmarks.ps1`，使用持久 ADB shell 从主机驱动真实启动、点击、滑动、输入和保存，不依赖后台 instrumentation 进程。
- benchmark-only Provider 为 UI 就绪信号分配代次；Compose 完成布局后写入包含目标 PID、信号名和代次的小文件。主机以原生 `cat` 读取，避免 Android `content` Java 工具约 1 秒的测量底噪。Release 不暴露 Provider，也不会配置或写入这些信号文件。
- vivo V2458A 四项各 30 次全部通过：冷启动 P95 808.399ms、万条流水首屏 227.074ms、切月 75.512ms、保存反馈 132.321ms；滚动趋势 P95 1234.836ms。
- `scripts/verify-target-device-benchmarks.ps1` 独立复算后输出 `overallPassed=true`，原始样本和执行日志已归档到目标真机证据目录。
- vivo V2458A 实际开启 TalkBack 后，设备持有人逐项点击首页、流水、金额输入、预算和趋势，确认均可正常阅读和操作；TC-I5-014 通过。
- vivo V2458A 的应用锁启用、后台/熄屏 30 秒重锁、取消后保持锁定、认证成功恢复及最近任务保护均由设备持有人确认正常；TC-I5-019 通过。
