# CR-006 API 36 测试报告

> 版本：v1.0  
> 执行日期：2026-07-28  
> 被测版本：`1.0.0-rc1` Debug / benchmark  
> 设备：PBK_API_36 模拟器，Android 16 / API 36

## 1. 结论

CR-006 的首页和统计页变更已在 API 36 模拟器完成自动化与视觉复核：

- App instrumentation 全量 31/31 通过，0 failure/error/skip；
- 首页展示当月全部流水，并按本地日期分组；日期组头包含星期、当日支出和收入小计；
- 统计页的支出/收入选择会联动 2×2 数据总览、单条趋势线、分类构成和周期对比；
- 趋势图显示人民币纵轴，分类圆环显示百分比，排行显示每类笔数、金额和占比；
- 五周期柱图逐柱显示顶部金额；
- 记账页保存按钮与统计页全局类型选择在长列表滚动场景下的设备测试已增强并通过。

## 2. 自动化结果

| 项目 | 结果 |
|---|---|
| `:app:connectedDebugAndroidTest` | 31/31，通过 |
| `InsightsScreensTest` 定向复核 | 2/2，通过 |
| `PhysicalDeviceComposeUiTest.transactionEditorSavesAndShowsFeedback` 定向复核 | 1/1，通过 |
| `:app:assembleBenchmark` | 通过 |
| benchmark 数据准备 | 10,000 笔，Provider 返回 `count=10000` |

全量连接测试命令：

```powershell
$env:GRADLE_USER_HOME='C:\Users\s5200\.gradle'
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 3. 视觉复核

### 3.1 首页按天聚合

- [本月汇总、流水总数与 7 月 28 日日期组](evidence/2026-07-28-cr006-home.png)
- [7 月 27 日日期组、当日支出 ¥1,225.25 / 收入 ¥0.00 与组内流水](evidence/2026-07-28-cr006-home-next-date.png)

两张连续滚动截图共同覆盖日期切换、日期组头、日汇总和组内流水。benchmark 首日样例为转账，因此首日支出/收入小计均为零；次日截图验证了非零支出小计。

### 3.2 统计收支联动

- [支出模式：月支出、日均支出、比上月支出、收支结余与支出趋势](evidence/2026-07-28-cr006-statistics-expense-top.png)
- [收入模式：月收入、日均收入、比上月收入、收支结余与收入趋势](evidence/2026-07-28-cr006-statistics-income-top.png)

两种模式各只展示所选类型的一条趋势线，纵轴均显示人民币金额刻度。

### 3.3 分类构成和周期对比

- [支出分类圆环、构成百分比、550 笔与金额](evidence/2026-07-28-cr006-statistics-expense-details.png)
- [近 5 周期柱图与逐柱顶部金额](evidence/2026-07-28-cr006-statistics-bar-values.png)

## 4. 测试稳定性修正

本轮没有修改生产行为，只增强了两个设备测试的滚动定位：

1. 统计页测试在点击收入筛选前，将 `statistics-type-income` 滚动到可见区域；
2. 物理设备兼容测试查找保存按钮时，最多执行 4 次受限上滑，兼容小屏和不同字体密度。

上述修正均由定向测试和全量 31 项连接测试复核。
