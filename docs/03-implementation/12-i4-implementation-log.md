# I4 实现日志

> 开始日期：2026-07-23
> 完成日期：2026-07-23
> 状态：已完成

## 输入与决策

- 用户批准进入 I4；范围按 [I4 详细设计](11-i4-detailed-design.md)执行。
- API 28 与 API 36 为强制设备矩阵；API 37 继续按 DEC-018 为可选前向兼容项。
- 完整备份采用严格 `.pbk` v1 归档，CSV 采用稳定 UTF-8/RFC 4180 契约。
- 应用锁复用系统认证，不创建或保存应用 PIN、生物特征及认证结果。
- Room schema v1 保持不变；设备安全配置放入独立 DataStore，不进入财务备份。

## 实际产物

- `backup/`：严格 JSON DTO、ZIP 编解码、SHA-256/字节数校验、资源上限、语义验证、Room 快照映射、恢复复核、事务恢复和 `pre-restore.pbk` 回滚快照。
- `export/`：日期闭区间查询、UTF-8 BOM、固定列、CRLF、RFC 4180 转义、两位正数金额和带偏移量时间。
- `security/`：DataStore 锁设置、30 秒单调时钟策略和系统认证协调。
- `ui/settings/`、`ui/security/`、`ui/privacy/`：数据与备份页、隐私与安全页、锁定页和全局金额遮罩。
- `MainActivity`：改为 `FragmentActivity`，接入 BiometricPrompt、冷启动/后台超时锁、认证不可用反馈和 `FLAG_SECURE`。
- `PortabilityDao`：一致性快照、全量恢复、CSV 范围查询和可携带偏好更新；未改变表、列、索引或触发器。
- 黄金 fixture、33 项 JVM 测试、20 项设备测试，以及 API 36 截图和真实 `.pbk` 冒烟文件。

## 实现与问题记录

1. Biometric 1.1.0 传递引入的旧 Fragment 1.2.5 在真实 SAF 启动时抛出“requestCode 只能使用低 16 位”。显式固定 Fragment 1.8.9，并增加由真实 `MainActivity` 启动/取消创建文档的设备回归；API 28/36 均通过。
2. 首次使用通用 `application/zip` MIME 时，DocumentsUI 将文件名改为 `.pbk.zip`。改用 `application/vnd.personalbookkeeping.backup` 后，实际保存文件保持 `.pbk`。
3. API 36 首次 20 项设备回归有 1 项失败：CSV 排序用例新增第二条记录后残留旧 `single()` 断言。实现已返回预期升序列表，只删除过期断言；API 28/36 最终各 20/20。
4. 收尾静态审计发现归档写出路径虽然计数，但未按 100 MiB 归档上限主动中止。补上写出限制，并新增压缩输入、解压数据、重复/额外/目录/穿越/缺失条目测试；JVM 最终 33/33。
5. API 36 人工冒烟成功通过 SAF 创建 `个人记账-2026-07-23.pbk`。归档大小 1,148 B，SHA-256 为 `A4A00D3ED3D18CF4395288B8EC7048AD4F5E86C5E12279EA859476966BF1D764`，只包含 `manifest.json` 和 `data.json`。
6. 全局金额遮罩在首页、流水、详情、统计、预算和账户只读展示中统一使用 `••••`；趋势图语义在遮罩状态下不包含真实金额。

## 验证结论

- JVM 33/33；API 28 20/20；API 36 20/20。
- Debug/Release 构建成功；Lint 0 error、10 条已知版本提示。
- Release Manifest 无网络和广泛存储权限；`allowBackup=false`，两代系统备份排除规则完整。
- Room schema v1 identity hash `7b70326941632b2446b9030ab01d0a44` 与文件 SHA-256 均未改变。
- `.pbk` 黄金样本、恶意归档、恢复回滚、CSV、金额遮罩、锁超时、无认证能力和真实 SAF 启动均有自动化证据。
- API 37 因本机内存限制未执行，不构成失败；无未关闭 P0/P1 缺陷。

完整结果见 [I4 测试报告](../04-testing/08-i4-test-report.md)。
