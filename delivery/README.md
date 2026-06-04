# 交付文件索引

本目录用于集中说明本次提交的交付材料位置，不移动后端、前端和测试源码，避免破坏 Maven、Vitest 的默认项目结构。

## SDD 核心文档

| 文档 | 位置 | 说明 |
| --- | --- | --- |
| 需求规格说明 | `../specs/spec.md` | 项目需求、业务规则和验收口径 |
| 技术实施计划 | `../specs/plan.md` | 架构设计、分层方案和技术路线 |
| 任务清单 | `../specs/tasks.md` | SDD 任务拆解和目标文件追踪 |
| 项目测试报告 | `../specs/test-report.md` | 全维度闭环验收测试报告 |

## 单元测试与集成测试

测试源码保持在各技术栈标准目录：

- 后端测试：`../backend/src/test/java/`
- 前端测试：`../frontend/src/**/*.spec.ts`

详细测试清单见 `unit-tests.md`。

## 验证命令

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run test:unit
npm run build
```

最后更新时间：2026-06-04 09:15:00
