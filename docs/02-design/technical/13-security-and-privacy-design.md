# 安全与隐私设计

> 版本：v1.0（已基线化）  
> 原则：无静默网络、最小权限、敏感内容默认留在应用私有空间

## 1. 威胁边界

| 场景 | MVP 应对 | 边界 |
|---|---|---|
| 其他普通 App 读取数据库 | Android 应用沙箱、内部存储 | 不防 root、恶意系统镜像或已解锁调试提取 |
| 他人短暂拿到已解锁手机 | 可选应用锁、后台遮罩、最近任务保护 | 应用锁不是数据库加密 |
| 系统自动上传应用数据 | 禁用 Auto Backup，并用提取规则排除云与设备迁移 | 用户主动在系统文件选择器选云盘属于用户明确行为 |
| 恶意/损坏备份 | 大小、条目、schema、摘要、引用和事务恢复校验 | 不执行备份中的代码或路径 |
| 日志泄露 | Release 日志裁剪，金额/备注/URI 脱敏 | 系统级内存取证不在范围内 |
| 依赖供应链 | 固定版本、依赖校验、最少第三方库 | 构建工具仓库本身仍需信任 |

## 2. Manifest 与权限

- 不声明 `android.permission.INTERNET`、网络状态、短信、通知、通讯录、位置、无障碍或悬浮窗权限。
- 不声明外部存储读写或“所有文件访问”；备份和 CSV 全部走 Storage Access Framework。
- 生物识别使用 AndroidX Biometric 所需普通权限。
- 除带 `MAIN/LAUNCHER` 的 `MainActivity` 必须 `exported=true` 外，其他组件默认 `exported=false`；MVP 不定义自有 Service、Provider 或 Receiver。
- 不接受外部 deep link；若后续增加，需独立输入与导出组件评审。

## 3. 禁用系统自动备份

Android Auto Backup 默认可能包含数据库、内部文件和偏好，并上传到用户 Google Drive。为满足“本地、无静默上传”：

- Manifest 显式设置 `android:allowBackup="false"`。
- Android 11 及以下的 `fullBackupContent` 规则排除所有 domain。
- Android 12 及以上的 `dataExtractionRules` 同时排除 cloud backup 与 device transfer 的数据库、文件和偏好。
- 数据库、回滚文件和设备状态均不得依赖系统自动备份。

部分厂商在 Android 12+ 上可能不完全遵循 `allowBackup=false` 对设备到设备迁移的语义，因此仍配置完整排除规则并在目标设备验证。

官方依据：[Android Auto Backup](https://developer.android.com/identity/data/autobackup)。

## 4. 应用锁

- 使用 `BiometricPrompt`；优先 `BIOMETRIC_STRONG`，允许设备凭据回退。
- Android 9/10 对生物识别与设备凭据组合存在平台限制，按 API 分支配置并在 API 28/29 实测。
- 未完成认证前只渲染遮罩，不先组合真实首页再覆盖。
- 启用应用锁时设置 `FLAG_SECURE`，防止截图和最近任务缩略图显示账本；关闭锁时允许用户正常截图。
- 后台超时由单调时钟计算，系统时间回拨不绕过锁。
- 不保存 PIN、图案、指纹或面容；DataStore 只保存“是否启用”和超时策略。

官方依据：[BiometricPrompt 官方指南](https://developer.android.com/identity/sign-in/biometric-auth)。

## 5. 本地数据

- Room 数据库、DataStore 与回滚备份位于内部私有目录。
- SQLite WAL/SHM 视为同等敏感；不复制到共享目录。
- 缓存中的备份中间文件使用随机名、`use`/`finally` 清理；启动时清理过期临时文件。
- 不把金额、备注、账户名、完整 URI、备份内容或生物识别结果写日志。
- Release 禁止 WebView 调试、调试备份入口和详细异常展示。

## 6. 外部备份

`.pbk` v1 未加密，完整性校验不等于保密。用户主动创建前显示一次明确说明，成功页再次展示文件位置由用户选择。应用不保存外部 URI 的永久访问权，除非未来明确增加自动备份需求。

若 P1 增加口令加密，必须采用经过评审的 AEAD（如 AES-GCM）与抗暴力 KDF，新增密码确认、忘记密码不可恢复、格式版本与黄金 fixture；不得使用自制加密或设备 Keystore 作为可移植备份的唯一密钥。

## 7. 安全验证清单

- 合并后的 Release Manifest 中不存在 `INTERNET` 和广泛存储权限。
- 依赖树中不存在网络、广告、分析、崩溃上传 SDK。
- `allowBackup=false` 与两代提取规则实际打包。
- 锁定状态的最近任务、旋转、后台恢复和认证失败不泄露内容。
- 恶意 ZIP：路径穿越、重复条目、压缩炸弹、超大 JSON、未知字段、悬空引用均被拒绝。
- Release 日志扫描不出现测试金额、备注、账户名或 URI。
