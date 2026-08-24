# P1 数据层与知识平台

> 更新时间：2026-08-24

## 模块

| 模块 | 职责 |
|------|------|
| `tft-data` | Source Adapter、Snapshot、Normalize、Patch、Evidence、Conflict、Quality |
| `tft-knowledge` | Knowledge Tools、RAG、LLM Gateway、Knowledge/Research Agent |

## P1 任务进度

| ID | 任务 | 状态 |
|----|------|------|
| P1-DATA-SourceAdapter-001 | Source Adapter SPI | DONE |
| P1-DATA-Snapshot-001 | Raw Snapshot Store | DONE |
| P1-DATA-Riot-001 | Riot/Data Dragon Adapter | DONE |
| P1-DATA-Stats-001 | 第一统计源（OP.GG MCP） | DONE |
| P1-DATA-Stats-002 | 第二统计源 | **BLOCKED** |
| P1-DATA-EntityResolve-001 | Canonical Entity Resolver | DONE |
| P1-DATA-Normalize-001 | Knowledge Normalizer | DONE |
| P1-DATA-Patch-001/002 | Patch Manager / Diff | DONE |
| P1-DATA-Evidence-001 | Evidence Store | DONE |
| P1-DATA-Conflict-001 | Conflict Queue | DONE |
| P1-DATA-Quality-001 | Source Quality Scoring | DONE |
| P1-KNOW-* | 8 个确定性 Knowledge Tools | DONE |
| P1-RAG-* | Ingest→Chunk→Embed→Vector→Hybrid→RAG API | DONE |
| P1-LLM-* | Gateway / Guard / Meter / Prompt | DONE |
| P1-AGENT-* | Knowledge + Research Agent v1 | DONE |

## 快速启动

```java
KnowledgePlatform platform = KnowledgePlatform.createDefault();
var response = platform.knowledgeAgent().answer(
        new KnowledgeAgent.KnowledgeAgentRequest(
                "What does interest gold rule look like at 50 gold?",
                "set17-16.16",
                "corr-demo",
                false,
                false));
```

## 存储说明

- **CI/离线**：InMemory Vector/BM25/Evidence/Conflict 实现，无外部 DB 依赖
- **生产**：MySQL（结构化知识/Evidence）、pgvector（向量）、OpenSearch（BM25）可通过 SPI 替换 InMemory 实现

## 搁置项

- **P1-DATA-Stats-002**：第二统计源仍 BLOCKED（LoLChess 无合规公开 API）
