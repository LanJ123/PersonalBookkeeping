# vivo V2458A 真机检查记录

> 日期：2026-07-26
> 被测版本：`1.0.0-rc1` Debug
> 结论：阶段性通过；功能自动化、性能、TalkBack 和锁屏/认证已通过，飞行模式和正式签名升级尚未执行

| 检查项 | 结果 | 证据/说明 |
|---|---|---|
| ADB 单真机识别 | 通过 | 仅 `10AF5F0K1Z002T4`，无模拟器混入 |
| Debug 首次安装与启动 | 通过 | `pbk-target-home.png` |
| 首页布局和空状态 | 通过 | 未见裁切、遮挡或崩溃 |
| 底部导航 | 通过 | 首页、流水、统计、设置均可打开；对应截图已归档 |
| 流水筛选入口 | 通过 | 搜索、收支/转账及更多筛选控件可到达 |
| 新增支出 | 通过 | 保存 ¥12.34 餐饮/现金支出，出现“已保存，本地账本已更新。” |
| 首页汇总对账 | 通过 | 支出 ¥12.34、结余 -¥12.34、1 笔，与流水卡片一致；`pbk-home-after-save.png` |
| 强制停止后数据持续性 | 通过 | 重启应用后 ¥12.34 流水及汇总仍存在 |
| SAF 创建入口与风险确认 | 通过 | 未加密提示和确认对话框正常 |
| Android DocumentsUI | 通过 | 进入系统文件选择器，默认名 `个人记账-2026-07-26.pbk`；`pbk-saf-provider.png` |
| SAF 取消 | 通过 | 取消后返回应用且无崩溃；`pbk-data-transfer-after-cancel.png` |
| 数据库与备份自动化 | 11/11 通过 | `physical-device-database-and-portability.xml`，0 failure/error/skip |
| Compose UI 自动化 | 5/5 通过 | 改用不初始化 Espresso 的 UiAutomator 兼容通道；`physical-compose-ui-automation.xml`，0 failure/error/skip |
| 性能 P95 | 通过 | 四项门禁各 30 次全部通过；独立验证器 `overallPassed=true`，详见 `performance/` |
| TalkBack 人工顺序 | 通过 | 服务实际开启；首页、流水、金额输入、预算和趋势逐项点击，设备持有人确认均可正常阅读和操作；详见 `talkback-test-log.md` |
| 锁屏/设备凭据/生物识别 | 通过 | 启用、后台/熄屏 30 秒重锁、取消保持锁定、成功恢复和最近任务保护均正常；详见 `app-lock-test-log.md` |
| 飞行模式全流程 | 未执行 | 避免未经确认中断用户连接 |
| 正式签名首次/升级安装 | 未执行 | 正式密钥尚未提供，Debug 签名不得替代 |

## 测试环境异常

`LargeFontAccessibilityTest` 在该设备上先后使用无 Activity 规则、通用 Activity 和专用 Debug Activity 复测，均停在 Compose 测试规则初始化阶段并导致 instrumentation 不返回。ADB 日志未显示应用业务异常或 Java 崩溃。真机自动化随后改用不初始化 Espresso 的 `AndroidJUnit4 + UiAutomator` 兼容通道并完成 5/5；完整 Compose 语义级测试仍由 API 28/API 36 模拟器承担。
