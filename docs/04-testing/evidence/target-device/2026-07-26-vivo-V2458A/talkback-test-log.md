# TalkBack 真机人工测试记录

> 日期：2026-07-26
> 设备：vivo V2458A，Android 16 / API 36
> 被测版本：`1.0.0-rc1` Debug
> 结论：通过

## 前置状态

- 系统 `accessibility_enabled=1`。
- 已启用并绑定 `com.google.android.marvin.talkback/.TalkBackService`。
- Debug 应用 `com.personalbookkeeping.app/.MainActivity` 在前台。
- 页面显示 TalkBack 绿色无障碍焦点框。

## 执行结果

| 页面/控件 | 结果 | 确认方式 |
|---|---|---|
| 首页 | 通过 | 焦点可见，内容可正常阅读 |
| 流水 | 通过 | 导航后朗读“流水”；设备持有人确认 |
| 金额输入 | 通过 | 输入控件可到达并可正常阅读 |
| 预算 | 通过 | 页面内容和操作可正常阅读 |
| 趋势 | 通过 | 图表相关内容可正常阅读 |

设备持有人逐项点击上述页面并明确确认“都可以正常阅读”。TC-I5-014 的“首页、流水、金额输入、预算、趋势可理解可操作”验收条件满足。

## 证据

- `talkback-home-initial.png`：首页焦点框。
- `talkback-tap-ledger.png`：流水导航焦点。
- `talkback-editor.png`、`talkback-amount-focus.png`：记账与金额输入。
- `talkback-ledger.xml`、`talkback-editor.xml`：运行时无障碍节点快照。

说明：ADB 注入的单指滑动在该 vivo ROM 上不能稳定等价于用户手势，自动驱动改用直接页面导航并保存焦点/节点证据；最终朗读与可操作性结论来自设备持有人在 TalkBack 实际开启状态下的逐项人工确认。
