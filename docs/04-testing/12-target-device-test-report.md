# 目标真机测试报告

> 版本：v0.5
> 执行日期：2026-07-26
> 被测版本：`1.0.0-rc1` Debug / benchmark
> 设备：vivo V2458A，Android 16 / API 36

## 1. 结论

目标真机功能自动化和性能阶段已完成：

- Debug 首次安装与启动通过；
- 数据库、备份与偏好层真机自动化 11/11 通过；
- Compose UI 真机黑盒自动化 5/5 通过，全程无需人工启动 App；
- 首页、流水、统计、设置、记账编辑器导航冒烟通过；
- ¥12.34 支出保存、首页汇总对账和强制停止后持久化通过；
- vivo 上的 Android DocumentsUI 创建入口、默认 `.pbk` 文件名与取消返回通过。
- 四项性能发布门各 30 次/P95 全部通过，三次固定滚动趋势已归档。
- TalkBack 服务在目标真机上确认启用；首页、流水、金额输入、预算和趋势由设备持有人逐项点击，朗读与操作均正常。
- 应用锁启用、后台/熄屏超时重锁、取消认证、成功解锁和最近任务隐私保护由设备持有人确认正常。

正式发布门仍有外部项未关闭：飞行模式和正式签名升级仍需用户交互或正式密钥。性能门已在设备充电、拔线降温后完成。

## 2. 自动化结果

| 项目 | 结果 |
|---|---|
| AppDatabaseTest | 7/7，通过 |
| PortabilityServiceTest | 4/4，通过 |
| 数据层合计 | 11/11，0 failure/error/skip |
| PhysicalDeviceComposeUiTest | 5/5，通过；0 failure/error/skip；52.706 秒 |
| 真机自动化合计 | 16/16，通过 |
| 真机性能门 | 四项各 30 次/P95 全部通过；滚动趋势 30 次已归档 |

测试报告 XML：

- [`physical-device-database-and-portability.xml`](evidence/target-device/2026-07-26-vivo-V2458A/physical-device-database-and-portability.xml)
- [`physical-compose-ui-automation.xml`](evidence/target-device/2026-07-26-vivo-V2458A/physical-compose-ui-automation.xml)

### 2.1 Compose UI 真机兼容方案

vivo Android 16 上，直接使用 Compose/Espresso rule 时会在 Espresso 安装主线程消息队列同步器阶段挂起，尚未进入 Compose 页面渲染；普通应用、数据库 instrumentation 和 API 36 模拟器均不复现。

真机通道因此改为 `AndroidJUnit4 + UiAutomator`，不初始化 Espresso，通过 Compose 语义树暴露的 resource id 操作真实页面。启动使用 shell 显式 `am start`，规避厂商 ROM 对测试进程后台启动 Activity 的限制。该通道覆盖：

1. 首页、流水、统计、设置导航；
2. “隐藏金额”开关与主题选项点击后的即时状态刷新，并恢复原状态；
3. 记账编辑器输入、保存和成功反馈；
4. 未加密备份风险提示及取消；
5. vivo `com.android.documentsui` 系统文件选择器启动、取消和返回。

完整 Compose 语义级测试继续由 API 28/API 36 模拟器承担；真机关键流程由上述厂商兼容通道承担。

### 2.2 性能自动化兼容方案

该 ROM 的标准 Macrobenchmark 通道存在三项设备兼容问题：

1. ProfileInstaller 安装配置和着色器缓存广播返回异常结果，AndroidX 在采样前失败；
2. UiAutomator 节点查询及 `UiDevice.click()` 输入注入可能永久不返回；
3. 独立 `com.personalbookkeeping.benchmark` 进程在目标 App 前台后被 vivo `fast_freezer` 冻结并随后杀掉。

正式采样改由 `scripts/run-target-device-benchmarks.ps1` 在电脑端维持单个 ADB shell，直接驱动启动、点击、滑动、输入和保存。benchmark-only Provider 在计时前重置信号代次；Compose 完成目标布局时写入包含目标 App PID、信号名和代次的小文件。主机在计时区间内只使用原生 `cat` 等待精确文件状态，因此不包含 Android `content` Java 工具约 1 秒的启动开销，也不依赖会被 ROM 冻结的后台测试进程。Release 不包含 Provider，且不会创建信号目录。

正式结果：

| 指标 | 样本 | 中位数 | P95 | 门槛 | 结论 |
|---|---:|---:|---:|---:|---|
| 冷启动 TTFD | 30 | 390.973ms | 808.399ms | ≤2000ms | 通过 |
| 10,000 条流水首屏 | 30 | 152.427ms | 227.074ms | ≤1000ms | 通过 |
| 月份切换 | 30 | 68.469ms | 75.512ms | ≤1000ms | 通过 |
| 保存至成功反馈 | 30 | 81.902ms | 132.321ms | ≤500ms | 通过 |
| 三次固定滚动 | 30 | 1150.030ms | 1234.836ms | 趋势项 | 已归档 |

独立验证器重新计算 P95、核对 30 样本和同一设备元数据后输出 `overallPassed=true`。开始时电量 52%、温度 37.1°C、Thermal Status 0；结束时电量 50%、温度 39.4°C、Thermal Status 1；全程未充电，旋转已恢复自动。

## 3. 人工冒烟结果

普通 Debug 应用没有复现 instrumentation 的无响应。底部四个导航入口、流水筛选、设置、数据与备份和记账页均可打开。新增餐饮/现金支出 ¥12.34 后：

- 保存成功反馈出现；
- 首页显示本月支出 ¥12.34、结余 -¥12.34、1 笔；
- 最近流水显示餐饮 -¥12.34；
- 强制停止并重新启动后结果仍一致。

SAF 创建流程显示未加密风险确认；继续后进入 `com.android.documentsui`，默认文件名为 `个人记账-2026-07-26.pbk`，取消可正常返回应用。

TalkBack 人工矩阵在服务实际开启状态下执行。应用显示无障碍焦点框，导航项“流水”朗读正确；设备持有人随后逐项点击首页、流水、金额输入、预算和趋势，确认均可正常阅读和操作。该结果关闭 TC-I5-014。

应用锁人工矩阵覆盖启用认证、离开应用 30 秒后的后台重锁、熄屏超过 30 秒后的重入、取消认证保持锁定、认证成功恢复原页面和最近任务预览保护。设备持有人完成操作并确认功能无异常；该结果关闭 TC-I5-019。

## 4. 未关闭门禁

| 门禁 | 原因 | 下一步 |
|---|---|---|
| 飞行模式 | 会中断用户连接 | 经确认后执行全核心流程 |
| 正式签名升级 | 缺少正式密钥 | 仓库外签名后验证首次/同签名升级与数据保留 |

## 5. 证据

证据目录：[`2026-07-26-vivo-V2458A`](evidence/target-device/2026-07-26-vivo-V2458A/)。

该目录包含设备信息、人工检查记录、TalkBack 与应用锁人工确认记录、TalkBack 焦点截图、数据层 11/11 XML、Compose UI 5/5 XML/日志、页面与 SAF 截图，以及 [`performance`](evidence/target-device/2026-07-26-vivo-V2458A/performance/) 下五项原始 JSON、30 次执行日志和独立汇总报告。
