# vivo V2458A 性能运行环境

- 执行时间：2026-07-26 17:56:54～18:02:22（Asia/Shanghai）
- 设备：vivo V2458A，Android 16 / API 36
- 构建指纹：`vivo/PD2415M/PD2415:16/BP2A.250605.031.A3_V000L1/compiler260529100608:user/release-keys`
- 被测包：`1.0.0-rc1` benchmark 变体，Release-like、R8、资源收缩、Debug 测试签名
- 数据：benchmark-only Provider 确定性预置 10,000 条流水
- 驱动：主机持久 ADB shell + benchmark-only PID/代次文件就绪信号
- 采样：冷启动、流水首屏、固定滚动、切月、保存反馈各 30 次
- 开始状态：电量 52%，未充电，37.1°C，Thermal Status 0
- 结束状态：电量 50%，未充电，39.4°C，Thermal Status 1
- 设备设置恢复：`wm user-rotation` 为 `free`
- 独立验证：`target-device-verification.json` 中 `overallPassed=true`

执行命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-target-device-benchmarks.ps1 `
  -Serial "192.168.0.115:5555" `
  -Iterations 30 `
  -OutputDirectory ".\docs\04-testing\evidence\target-device\2026-07-26-vivo-V2458A\performance"
```

## SHA-256

```text
i5-cold-start-ttfd.json                  618B4339529C952FBE1DDD9AB1FD04EA5E93E23EF480583B01639CAE27BC87ED
i5-ledger-first-content-10k.json         26FB6E4E23FFC14D65D0958E1AA1D99D2EE13E56FC7680E17E5CD896BB0A8F7C
i5-ledger-scroll-10k.json                A8886ACDB716F8468680A34242EC8D89E7C3737DB5857FB2BC1193EA4B114BA1
i5-previous-month-switch.json            322E5873854F60A4128CC624DD0C30C98BB9D17529AB1BEDC2F660A6C9FD1467
i5-save-feedback-10k.json                FDEB4C9EA0FB83FB44E598954EC7C1EA112E37EA92D2E0FF98D01481A35DA6DA
target-device-30.stdout.log              CEC8D83B1300E6B01AAA8ED12773451C61EB8B95AEB598A8B7EB055C62E69B53
target-device-verification.json          970F81919E26D9427E0618391962CE2286EE57E12797D9E3097F2ADDB54E8D93
```
