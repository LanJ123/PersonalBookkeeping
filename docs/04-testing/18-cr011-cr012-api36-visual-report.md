# CR-011 / CR-012 API 36 视觉验证报告

> 日期：2026-07-30  
> 设备：PBK_API_36（Android 16 / API 36 模拟器）  
> 范围：流水详情、设置下五个二级页面  
> 真机状态：未连接、未操作

## 1. 验证结论

- 流水详情、账户、分类、预算、数据备份和隐私页面均使用相同起始位置的内容区大标题。
- 页面内不再显示“‹ 返回”；Android 系统返回手势仍由导航栈处理。
- 账户、分类、预算的新增入口均位于右下角，并显式使用圆形按钮。
- 账户和分类标题区域不再显示“新增”文字。
- 预算页已移除 `Scaffold` 重复注入的顶部系统安全区，标题位置与其他二级页面一致。

## 2. 自动化结果

| 项目 | 结果 |
|---|---|
| `testDebugUnitTest assembleDebug assembleDebugAndroidTest` | 通过 |
| App 定向 Compose UI 测试 | 11/11 通过 |
| 圆形 FAB 回归（账户、分类、预算） | 5/5 通过 |
| 预算顶部安全区回归 | 3/3 通过 |

定向测试覆盖 `TransactionDetailScreenTest`、`ManagementScreensTest`、`SettingsScreensTest` 和 `InsightsScreensTest`。根工程任务曾把 App 测试类名同时传入 benchmark 模块，造成 benchmark 侧类不存在的无关失败；改用 `:app:connectedDebugAndroidTest` 后结果全部通过。

## 3. 截图证据

| 页面 | 文件 | SHA-256 |
|---|---|---|
| 流水详情 | `2026-07-30-cr011-api36-detail.png` | `E34314038F57C6B9364627D030B58CC41464E484057B2342EA75B4F81AD5D815` |
| 账户管理 | `2026-07-30-cr012-api36-accounts.png` | `BA0967898339627850980D487FC0BEF461C7B103E197D099A9A7FBDE76343961` |
| 分类管理 | `2026-07-30-cr012-api36-categories.png` | `5E48E0EEBF7263BE14FB6BD860F47E4652D8EC538D5ABDA4D7B0251DAEC602B0` |
| 预算管理 | `2026-07-30-cr012-api36-budgets.png` | `382B9103DB4E1094ECC9BCFF27A1EC9E5D160E94B487C76E87F02F7392F59FB5` |
| 数据与备份 | `2026-07-30-cr012-api36-data.png` | `179C030CF8D27423BE274D34F2ED2CEC8331D4E7AC429866F82C54314551BDA9` |
| 隐私与安全 | `2026-07-30-cr012-api36-privacy.png` | `19B8F9704A56C6C78875D334AD720E7B8AFEA834B16E211FFBD4CBBBC1291434` |

## 4. 边界

- 本轮按用户要求仅进行模拟器验证，没有重新连接 vivo 真机。
- 模拟流水详情截图使用 benchmark 变体在模拟器本地生成的样例数据，不包含用户真机数据。
