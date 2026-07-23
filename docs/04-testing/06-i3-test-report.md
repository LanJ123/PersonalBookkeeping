# I3 测试报告

> 版本：v1.0
> 执行日期：2026-07-22～2026-07-23
> 结论：通过；I3 退出条件满足

## 1. 范围与环境

I3 覆盖首页月概览、统计排行/趋势/下钻、总预算与分类预算，以及 I1/I2 全量回归。平台保持 `minSdk=28`、`compileSdk=36`、`targetSdk=36`；API 28/36 AVD 均使用 x86_64、1.5 GB 内存和无窗口模式。API 37 按 DEC-018 未执行且不计失败。

没有新增运行时依赖。构建继续使用 AGP 9.3.0、Gradle 9.5.0、JDK 17、Kotlin 2.3.21、KSP 2.3.9、Room 2.8.4、Navigation 3 1.1.1 和 Paging 3.5.0。

## 2. 结果汇总

| 范围 | 结果 | 结论 |
|---|---|---|
| JVM 单元测试 | 18 通过，0 失败 | 通过 |
| API 28 instrumented | 11 通过，0 失败 | 通过 |
| API 36 instrumented | 11 通过，0 失败 | 通过 |
| Debug APK | `assembleDebug` 成功 | 通过 |
| Release APK | `assembleRelease` 与 Lint Vital 成功；未签名 | 通过（开发迭代） |
| Android Lint | 0 error，10 条已知版本提示 | 通过 |
| API 36 启动与人工冒烟 | 安装/启动、首页、预算保存/回流、共享月份和设置入口 | 通过 |
| P0/P1 缺陷 | 0 个未关闭 | 通过 |

2026-07-23 在最终源码状态再次执行强制设备矩阵：API 36 的 `testsuites` 记录 11 tests、0 failures、0 errors、退出码 0；API 28 的 Gradle 输出记录 11/11 与 `BUILD SUCCESSFUL`。

## 3. 功能与一致性证据

- JVM 用例验证 2024 闰年二月、月末闭区间、跨年切月及预算 79.99%、80%、99.99%、100% 和超额边界。
- Room 用例在同一自然月写入两笔支出、一笔收入和一笔转账，并写入上月支出隔离样本；月支出等于分类合计和日趋势支出合计，月收入等于日趋势收入合计。
- 上述用例证明转账不进入收支汇总，收入/转账/上月流水不进入本月总预算和分类预算消耗。
- 同作用域预算新增、编辑和清除成功；总预算与分类预算按各自事实金额独立计算。
- Compose 用例验证首页空状态、80% 明确文字提示、分类金额/占比和趋势文字替代信息。
- API 36 人工冒烟从首页进入预算页，保存 ¥1,000 后预算页与首页均立即显示“预算正常”；从首页选择 2026 年 6 月后进入统计，所选月份保持为 2026 年 6 月。
- 统计下钻通过 `LedgerViewModel.showMonth` 固定同月闭区间、`EXPENSE` 和分类 ID；数据库组合筛选继续由 I2 回归覆盖。

## 4. schema、隐私与依赖审计

- Room schema version 仍为 1，6 张表、2 个视图；identity hash `7b70326941632b2446b9030ab01d0a44`。
- schema SHA-256 为 `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504`，与 I1/I2 完全相同；Git 无 schema diff。
- 合并 Release Manifest 只有应用自身动态接收器签名权限，不含 `android.permission.INTERNET`。
- `android:allowBackup="false"` 保持不变，备份排除规则未改动。
- I3 未增加依赖，趋势图由 Compose Canvas 实现；Gradle 依赖校验继续生效。

## 5. 构建与证据产物

| 产物 | 大小 | SHA-256 |
|---|---:|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 33,066,623 B | `268727B006643272649958DEB87271DA2B1C5ECF22161E908D1EE472ACDF2802` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 25,186,144 B | `EFEC32C08E36018CFC51CF4CFB38B39E294825958D6CF877EB5500FC8A0ADA9D` |
| `app/schemas/com.personalbookkeeping.database.AppDatabase/1.json` | — | `01C2D0EBAF33AAE8AAD6D4B3FB252741DEA25434A0C008E505CEA7697B05C504` |
| [API 36 首页截图](evidence/2026-07-22-i3-api36-home.png) | 129,645 B | `9CFA2D025190080FB132343F31F984C3D03F35D1DA806CA191C0837476D32A03` |
| [API 36 预算截图](evidence/2026-07-22-i3-api36-budget.png) | 129,551 B | `277606BF4FC26077FEFCF2133C91D07C78339E1BB42FBCC424F1FD1AE4A9B736` |

`app/build/` 为可再生产物且不提交；源码、Room schema、测试、文档和截图永久保存。正式签名 APK 在发布阶段生成。

## 6. 已知提示与退出结论

Lint 的 10 条提示均为已评审的版本可用性信息，不含 error；升级相关工具或依赖会改变已签署的 API 36 技术基线，本迭代不处理。

API 28 首轮唯一失败为测试断言错误：7000/8000 实为 87.5%，实现正确返回“接近预算”；修正期望后 API 28 与 API 36 均全绿。最终无未关闭 P0/P1 缺陷，I3 完成；I4 需用户确认后开始。
