# 架构决策记录（ADR）

| ADR | 决策 | 状态 |
|---|---|---|
| [ADR-001](ADR-001-single-module-layered.md) | 单应用模块 + 包分层 | 已接受 |
| [ADR-002](ADR-002-room-source-of-truth.md) | Room 作为业务数据唯一事实源 | 已接受 |
| [ADR-003](ADR-003-portable-logical-backup.md) | 使用版本化逻辑备份而非 SQLite 副本 | 已接受 |
| [ADR-004](ADR-004-manual-dependency-injection.md) | MVP 使用手工依赖注入 | 已接受 |
| [ADR-005](ADR-005-disable-system-auto-backup.md) | 禁用系统自动备份和设备迁移 | 已接受 |
| [ADR-006](ADR-006-stable-api-36-baseline.md) | min 28、compile/target 36，API 37 仅可选兼容检查 | 已接受 |

技术设计 v1.0 已于 2026-07-22 确认。被替代的 ADR 不删除，改为“已取代”并链接新 ADR。
