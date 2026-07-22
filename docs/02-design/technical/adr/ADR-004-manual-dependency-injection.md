# ADR-004：MVP 使用手工依赖注入

- 状态：已接受
- 日期：2026-07-21

## 背景

对象图规模小，主要是数据库、repository、文件服务、Clock、dispatcher 和 ViewModel。

## 决策

由 Application 级 `AppContainer` 显式构造依赖，ViewModel factory 构造屏幕状态持有者，不引入 Hilt/Dagger。

## 后果

构建插件更少、依赖关系显式、测试替换直接；随着对象图扩大可能出现样板代码。若构造图明显难以维护，再评估 DI 框架并新增 ADR。
