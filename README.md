# PersonalBookkeeping

个人使用、完全离线的 Android 记账应用。

当前状态：**需求、UX 与技术设计 v1.0 均已确认；第 4 阶段 I0～I2 已通过，等待确认是否进入 I3（首页、统计与预算）。**

## 文档入口

- [项目文档索引](docs/README.md)
- [产品需求规格（PRD）](docs/01-requirements/02-product-requirements.md)
- [待确认问题](docs/01-requirements/07-assumptions-risks-open-questions.md)
- [开发流程与阶段门](docs/00-project/01-development-process.md)
- [UX 设计规格](docs/02-design/ui/01-ux-design-spec.md)
- [技术栈与构建基线](docs/02-design/technical/09-technical-stack.md)
- [系统架构](docs/02-design/technical/10-system-architecture.md)
- [数据库设计](docs/02-design/technical/11-database-design.md)
- [备份与导出格式](docs/02-design/technical/12-backup-and-export-design.md)
- [测试策略](docs/02-design/technical/14-test-strategy.md)
- [技术设计基线签署记录](docs/02-design/technical/17-technical-design-baseline.md)
- [迭代实现计划](docs/03-implementation/03-iteration-plan.md)
- [构建与运行说明](docs/03-implementation/05-build-and-run.md)
- [I1 测试报告](docs/04-testing/02-i1-test-report.md)
- [I2 测试报告](docs/04-testing/04-i2-test-report.md)

## 产品原则

1. 本地优先：核心功能无网络可用，默认不申请网络权限。
2. 数据归用户：支持完整备份、恢复和通用格式导出。
3. 快速记账：高频操作少步骤、低认知负担。
4. 金额可信：使用最小货币单位存储，转账保持原子一致性。
5. 克制范围：首版不做登录、云同步、理财推荐和自动记账。
