# CR-009 应用图标 API 36 验证报告

> 版本：v1.0
> 执行日期：2026-07-30
> 被测版本：本地 `debug` 构建
> 设备：PBK_API_36 模拟器，Android 16 / API 36，1080 × 2400

## 1. 结论

用户确认的 A 方案已重绘为 Android 原生自适应图标：

- 蓝色渐变背景与当前应用系统蓝视觉一致；
- 白色票据和金色人民币币在应用抽屉小尺寸下仍可辨识；
- API 36 圆形遮罩下主体完整，票据文字线、底部折角和金币均未被裁掉；
- Android 13 及以上单色主题图标资源继续有效；
- 应用名称、包名和启动配置保持不变。

## 2. 资源实现

| 资源 | 用途 |
|---|---|
| `drawable/ic_launcher_background.xml` | 自适应图标蓝色渐变背景 |
| `drawable/ic_launcher_foreground.xml` | 白色票据、蓝色账目线与金色人民币币 |
| `drawable/ic_launcher_monochrome.xml` | Android 13 主题图标蒙版 |
| `mipmap-anydpi-v26/ic_launcher*.xml` | Android 8+ 自适应启动图标 |
| `mipmap-anydpi-v33/ic_launcher*.xml` | Android 13+ 带单色层启动图标 |

## 3. 构建与安装

- `:app:compileDebugKotlin :app:assembleDebug`：通过；
- `:app:installDebug`：通过；
- Debug APK 成功安装至 PBK_API_36；
- Pixel Launcher 应用抽屉成功显示新图标和“个人记账”名称。

## 4. 视觉证据

| 证据 | SHA-256 |
|---|---|
| [用户确认的 A 方案视觉源](../02-design/ui/assets/cr009-app-icon-concept-a.png) | `DA201F3E2FC738FBFBEF6A6AE1DFEB1F55425C748F386D63DB66DE1251C98A27` |
| [API 36 应用抽屉圆形遮罩截图](evidence/2026-07-30-cr009-app-drawer.png) | `E0960E4062B05D821EA244AF87C3773A749242DD8255D34FDA791F1F1E99FB89` |

## 5. 边界

- 候选图由图像生成工具用于方向确认，正式应用资源已用确定性矢量重绘；
- 启动器最终遮罩和主题着色由设备桌面决定，因此不同厂商可能显示为圆角矩形、圆形或其他形状；
- 发布前仍建议在至少一台厂商真机上抽检主题图标颜色。
