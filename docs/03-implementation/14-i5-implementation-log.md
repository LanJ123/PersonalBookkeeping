# I5 实现日志：硬化与发布候选

> 版本：v1.0
> 完成日期：2026-07-23
> 结论：本地工程硬化完成；正式签名与目标真机门禁待外部条件

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
2. 在目标真机执行性能 P95、TalkBack、系统锁屏、SAF、飞行模式和厂商 ROM 复验。
3. 对正式签名 APK 归档 SHA-256、安装/升级与备份恢复结果。
