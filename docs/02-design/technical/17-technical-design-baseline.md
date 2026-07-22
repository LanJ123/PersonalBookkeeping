# 技术设计基线签署记录

> 基线版本：v1.0  
> 签署日期：2026-07-22  
> 结论：通过，允许进入第 4 阶段迭代实现

## 1. 确认依据

用户于 2026-07-22 确认进入第 4 步迭代实现。该确认连同此前对平台基线、API 37 可选策略及环境验收结果的确认，视为对本技术设计基线的批准。

## 2. 基线范围

- `09-technical-stack.md` 至 `16-technical-traceability.md`。
- `database-schema-v1.sql` 数据库参考 DDL。
- `schemas/` 下 `.pbk` v1 的两份 JSON Schema。
- `adr/ADR-001` 至 `ADR-006` 及 ADR 索引。
- 环境验收报告 `docs/03-implementation/02-environment-verification.md`。

## 3. 生效约束

- `minSdk=28`、`compileSdk=36`、`targetSdk=36`。
- Android Gradle Plugin 9.3.0、Gradle 9.5.0、Kotlin 2.3.21、KSP 2.3.9、Java toolchain 17。
- 单 `:app` 产品模块、Compose、Room、手工依赖注入、本地优先且默认无网络权限。
- 金额统一使用 `Long` 分；Room 是业务数据唯一事实源；备份为版本化逻辑 `.pbk`。
- API 28 与 API 36 为强制兼容矩阵；API 37 仅为可选前向兼容检查，不构成环境、初始化或发布门槛。

## 4. 未阻塞的实现期事项

- 首次真机验证前补录主力手机型号与 Android 版本。
- 发布签名前确定正式签名密钥和离线保管位置。
- AndroidX 辅助依赖仅在对应垂直切片引入，并固定精确稳定版本。

以上事项不改变架构和 MVP 范围，不阻塞工程初始化。后续破坏数据库、备份或需求兼容性的修改必须通过决策日志和正式变更记录。
