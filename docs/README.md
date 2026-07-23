# 项目文档索引

> 文档版本：v2.4；更新时间：2026-07-23；阶段：第 4 阶段迭代实现（I0～I3 已通过，等待 I4 授权）。

## 00 项目管理

| 文档 | 用途 | 状态 |
|---|---|---|
| [开发流程与阶段门](00-project/01-development-process.md) | 定义每阶段活动、产物与进入/退出条件 | 第 4 阶段；I3 已通过 |
| [决策日志](00-project/02-decision-log.md) | 保存关键产品和技术决策 | 持续更新 |
| [变更记录](00-project/03-change-log.md) | 保存基线后的正式变更及影响分析 | 持续更新 |

## 01 需求分析

| 文档 | 用途 | 状态 |
|---|---|---|
| [竞品与市场调研](01-requirements/01-market-research.md) | 提炼主流记账产品的通用模式 | 已完成 |
| [产品需求规格（PRD）](01-requirements/02-product-requirements.md) | 定义目标、范围、功能和版本 | v1.0 已基线化 |
| [用户故事与验收标准](01-requirements/03-user-stories-and-acceptance.md) | 将需求转为可测试行为 | v1.0 已基线化 |
| [信息架构与核心流程](01-requirements/04-information-architecture.md) | 定义页面结构与关键交互路径 | v1.0 已基线化 |
| [数据需求](01-requirements/05-data-requirements.md) | 定义核心实体、规则和备份要求 | v1.0 已基线化 |
| [非功能需求](01-requirements/06-non-functional-requirements.md) | 定义隐私、性能、兼容性等约束 | v1.0 已基线化 |
| [假设、风险与确认记录](01-requirements/07-assumptions-risks-open-questions.md) | 保存假设、风险与用户确认结果 | 已确认 |
| [需求追踪矩阵](01-requirements/08-requirements-traceability.md) | 关联需求、用户故事与验证方式 | 已建立 |
| [需求基线签署记录](01-requirements/09-requirements-baseline.md) | 固化 v1.0 范围、确认结果与变更规则 | 已签署 |

## 02 设计阶段

[设计文档目录说明](02-design/README.md)定义 UI/UX 与技术设计的存放边界。

### UI/UX 设计

| 文档 | 用途 | 状态 |
|---|---|---|
| [UX 设计规格](02-design/ui/01-ux-design-spec.md) | 定义导航、设计原则和核心交互 | v1.0 已基线化 |
| [页面与状态清单](02-design/ui/02-screen-inventory-and-states.md) | 确保正常、空、错、锁定等状态完整 | v1.0 已基线化 |
| [核心交互流程](02-design/ui/03-interaction-flows.md) | 固化记账、编辑、预算和恢复路径 | v1.0 已基线化 |
| [低保真线框图](02-design/ui/04-low-fidelity-wireframes.md) | 描述主要页面布局 | v1.0 已基线化 |
| [内容与无障碍规范](02-design/ui/05-content-and-accessibility.md) | 统一文案、触控和可访问性 | v1.0 已基线化 |
| [UX 评审记录](02-design/ui/06-ux-review.md) | 保存自检结论、风险和阶段门 | 已通过 |
| [设计追踪矩阵](02-design/ui/07-design-traceability.md) | 将 UX 决策映射到需求 | 已建立 |
| [UX 设计基线签署记录](02-design/ui/08-ux-design-baseline.md) | 固化 v1.0 设计范围与确认结果 | 已签署 |

### 技术设计

| 文档 | 用途 | 状态 |
|---|---|---|
| [技术栈与构建基线](02-design/technical/09-technical-stack.md) | 固化平台、语言、框架和版本 | v1.0 已基线化 |
| [系统架构](02-design/technical/10-system-architecture.md) | 定义分层、包结构、依赖与关键流程 | v1.0 已基线化 |
| [数据库设计](02-design/technical/11-database-design.md) | 定义表、关系、索引和一致性规则 | v1.0 已基线化 |
| [数据库参考 DDL](02-design/technical/database-schema-v1.sql) | 保存可审计的 schema 参考 | v1.0 基线附件 |
| [备份与导出设计](02-design/technical/12-backup-and-export-design.md) | 定义 `.pbk`、恢复流程和 CSV 契约 | v1.0 已基线化 |
| [备份 manifest JSON Schema](02-design/technical/schemas/backup-manifest-v1.schema.json) | 机器可读的备份清单契约 | v1.0 基线附件 |
| [备份数据 JSON Schema](02-design/technical/schemas/backup-data-v1.schema.json) | 机器可读的业务数据契约 | v1.0 基线附件 |
| [安全与隐私设计](02-design/technical/13-security-and-privacy-design.md) | 定义威胁边界、权限与敏感数据控制 | v1.0 已基线化 |
| [测试策略](02-design/technical/14-test-strategy.md) | 定义测试层次、矩阵和发布门槛 | v1.0 已基线化 |
| [技术风险与评审](02-design/technical/15-technical-risk-and-review.md) | 保存风险、自检和阶段门 | v1.0 已通过 |
| [技术追踪矩阵](02-design/technical/16-technical-traceability.md) | 映射需求、设计模块与验证 | v1.0 已基线化 |
| [技术设计基线签署记录](02-design/technical/17-technical-design-baseline.md) | 固化技术范围、确认依据与实现约束 | v1.0 已签署 |
| [架构决策记录目录](02-design/technical/adr/README.md) | 保存技术取舍及其后果 | ADR-001～006 已接受 |

## 03 迭代实现

| 文档 | 用途 | 状态 |
|---|---|---|
| [Windows Android 开发环境准备指南](03-implementation/01-environment-setup-windows.md) | 记录当前环境审计、安装步骤和验收命令 | v0.1 已完成 |
| [Windows Android 开发环境验收报告](03-implementation/02-environment-verification.md) | 保存实际安装版本、AVD 配置与剩余验证项 | v1.1；必需环境通过 |
| [迭代实现计划](03-implementation/03-iteration-plan.md) | 定义垂直切片、验收门槛和交付顺序 | v1.2；I3 已通过 |
| [编码规范](03-implementation/04-coding-standards.md) | 统一 Kotlin、Compose、Room、隐私和测试约束 | v1.0；已生效 |
| [构建与运行说明](03-implementation/05-build-and-run.md) | 保存本机构建、测试、安装和排错命令 | v1.0；持续更新 |
| [实现日志](03-implementation/06-implementation-log.md) | 逐次保存代码产物、验证结果和遗留事项 | 持续更新 |
| [I2 详细设计](03-implementation/07-i2-detailed-design.md) | 固化导航、筛选、管理、编辑删除与撤销契约 | v1.0；已批准实现 |
| [I2 实现日志](03-implementation/08-i2-implementation-log.md) | 保存 I2 代码、测试和问题处理记录 | v1.0；已完成 |
| [I3 详细设计](03-implementation/09-i3-detailed-design.md) | 固化月份、聚合、统计下钻与预算契约 | v1.0；已实施 |
| [I3 实现日志](03-implementation/10-i3-implementation-log.md) | 保存 I3 代码、测试和问题处理记录 | v1.0；已完成 |

## 04 测试

| 文档/产物 | 用途 | 状态 |
|---|---|---|
| [I1 测试用例](04-testing/01-i1-test-cases.md) | 定义金额、交易、数据库、UI、构建与隐私检查 | v1.0；已执行 |
| [I1 测试报告](04-testing/02-i1-test-report.md) | 保存 API 28/36、单测、Lint、构建和校验结论 | v1.0；通过 |
| [API 36 运行截图](04-testing/evidence/2026-07-22-i1-api36.png) | 保存 I1 实机界面冒烟证据 | 已归档 |
| [I2 测试用例](04-testing/03-i2-test-cases.md) | 定义管理、流水、设备、构建与隐私验证 | v1.0；已执行 |
| [I2 测试报告](04-testing/04-i2-test-report.md) | 保存 I2 自动化、设备矩阵、哈希和退出结论 | v1.0；通过 |
| [I2 API 36 运行截图](04-testing/evidence/2026-07-22-i2-api36.png) | 保存 I2 流水页视觉冒烟证据 | 已归档 |
| [I3 测试用例](04-testing/05-i3-test-cases.md) | 定义月份、聚合、预算、页面、设备与审计验证 | v1.0；已执行 |
| [I3 测试报告](04-testing/06-i3-test-report.md) | 保存 I3 自动化、设备矩阵、哈希和退出结论 | v1.0；通过 |
| [I3 API 36 首页截图](04-testing/evidence/2026-07-22-i3-api36-home.png) | 保存 I3 首页空状态视觉证据 | 已归档 |
| [I3 API 36 预算截图](04-testing/evidence/2026-07-22-i3-api36-budget.png) | 保存 I3 预算保存视觉证据 | 已归档 |

## 后续阶段预留目录

- `docs/02-design/ui/` 后续按实现需要补充视觉 token。
- `docs/03-implementation/` 持续补充迭代结果、构建记录和代码变更。
- `docs/04-testing/` 后续按迭代补充测试用例、报告与缺陷记录。
- `docs/05-release/`：发布清单、APK 校验信息、版本说明、用户手册。
- `docs/06-maintenance/`：变更请求、问题复盘、版本路线图。

所有需求变更先更新 PRD、决策日志和追踪矩阵，再进入实现。
