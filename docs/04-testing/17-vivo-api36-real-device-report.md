# vivo Android 16 真机测试报告

> 版本：v1.0
> 执行日期：2026-07-30
> 被测版本：本地 `debug` 构建，包含 CR-009 与 CR-010
> 连接方式：ADB 无线调试

## 1. 设备环境

| 项目 | 值 |
|---|---|
| 厂商 / 型号 | vivo V2458A（PD2415） |
| Android / API | Android 16 / API 36 |
| 屏幕 | 1260 × 2800，560 dpi |
| 字体缩放 | 1.0 |
| 测试开始电量 | 76% |
| 应用版本 | 1.0.0-rc1 Debug |

配对前设备上未安装 `com.personalbookkeeping.app`，因此不存在覆盖用户正式账本的风险。测试结束后已重新安装 Debug 版本，供继续手动测试。

## 2. 结论

当前代码在 vivo Android 16 真机上的适配结果：

- Debug APK 构建、无线安装和冷启动通过；
- 首页、流水、统计、设置、记账五个核心页面未出现状态栏重复留白、底栏遮挡或固定内容截断；
- 新记账页只显示内容区大标题，不显示返回按钮和迭代说明；
- 从首页、流水、统计、设置四个来源分别保存流水后，均自动返回对应来源；
- 数据库约束、余额派生、筛选、统计聚合、备份恢复、CSV 查询等 11 项数据测试通过；
- 真机专用导航、隐私设置、记账、备份警告与系统文件选择器 5 项 UI 测试通过。

未发现可复现的产品崩溃或阻断缺陷。

## 3. 自动化结果

| 测试范围 | 结果 | 说明 |
|---|---|---|
| `testDebugUnitTest + assembleDebug + assembleDebugAndroidTest` | 通过 | 主程序、单元测试和测试 APK 均成功构建 |
| `AppDatabaseTest` | 7/7 通过 | 数据约束、余额、分页筛选、统计和编辑删除恢复 |
| `PortabilityServiceTest` | 4/4 通过 | 备份恢复回滚、偏好、CSV 日期范围 |
| `PhysicalDeviceComposeUiTest` | 5/5 通过 | 四主入口、隐私/主题、记账、备份警告、系统文件选择器 |
| 四来源保存后返回专项 | 1/1 通过 | 首页、流水、统计、设置各保存一次并返回原来源 |

### vivo 测试运行器兼容性

首次直接运行全部 32 项通用 instrumentation 时，前 11 项数据测试通过；第 12 项 `LargeFontAccessibilityTest` 等待了约 106 秒，用户手动打开 App 后才继续，随后 runner 报告 `Process crashed`。日志中没有应用 `FATAL EXCEPTION`，Activity 仅在手动打开后显示。

项目已有的 `PhysicalDeviceComposeUiTest` 明确用于绕开部分 Android 16 厂商系统上 Espresso/Compose 主循环同步器的死锁。本报告采用该真机专用通道完成 UI 验收，不把厂商 runner 启动问题记作产品崩溃，也不把未执行的通用用例误报为通过。通用 Compose 大字体用例已在 API 36 模拟器通道覆盖。

测试期间出现 `androidx.test.services` 不存在导致的 `appops` 警告，但未影响已选测试执行和最终 Gradle 成功状态。

## 4. 真机视觉证据

| 页面 | 覆盖内容 | SHA-256 |
|---|---|---|
| [首页](evidence/2026-07-30-vivo-home-final.png) | 大标题、月度概览、空流水状态、底部导航 | `CC7BFEBCA14BDDFC6FD7B936FECE4091DE426BA83D4EC345F9B3563337258BA2` |
| [流水](evidence/2026-07-30-vivo-ledger.png) | 大标题、备注搜索、月份与类型筛选、空状态 | `05B905ECEBE2F8B9BB3A50C5F332169820CA48C7105E829F90DF3E3E5BCB40C9` |
| [统计](evidence/2026-07-30-vivo-statistics.png) | 周/月/年、支出/收入、2×2 汇总、金额纵轴 | `51AD7B64FEC8774694A4EAA7DEF6A4D6D1E69D39C622DBBC3CE8AD5BFF4B2ACB` |
| [设置](evidence/2026-07-30-vivo-settings.png) | 设置页大标题、分组列表、底栏 | `84093AC91B906DBD1170490522B6FF43EB64DF1A7E1DFDBD7C8890B5140B84F8` |
| [记账 CR-010](evidence/2026-07-30-vivo-editor-cr010.png) | 新大标题、无返回/迭代说明、完整表单 | `E2E53EE5DD1EAC40BE73238285681829ABDF5C4F47AD5B36992FDC604242B562` |

## 5. 数据安全与未执行项

- 未执行 benchmark 的 10,000 条真机数据灌入，因为该 Provider 会先清空同包数据库；在没有用户明确授权清库前不应运行。
- 有数据的日期分组、分类构成百分比、柱顶金额和大量列表性能继续以 API 36 模拟器的既有 CR-006/007/008 证据为准。
- 真机专用 UI 用例通过正常页面操作创建测试流水，不使用数据库直写或绕过业务校验。
- 若需要在这台手机上进行 10,000 条压力测试，应先明确授权清空当前测试包数据；不要在保存真实账本后运行。
