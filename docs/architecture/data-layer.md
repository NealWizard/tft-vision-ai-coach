# P1 数据层与知识平台

> 更新时间：2026-08-26 16:49 +08:00

## 模块

| 模块 | 职责 |
|------|------|
| `tft-data` | Source Adapter、Snapshot、Normalize、Patch、Evidence、Conflict、Quality；MySQL JDBC 实现 |
| `tft-knowledge` | Knowledge Tools、RAG（InMemory/ES）、LLM Gateway、Knowledge/Research Agent |

## P1 任务进度

| ID | 任务 | 状态 |
|----|------|------|
| P1-DATA-SourceAdapter-001 | Source Adapter SPI | DONE |
| P1-DATA-Snapshot-001 | Raw Snapshot Store | DONE |
| P1-DATA-Riot-001 | Riot/Data Dragon Adapter | DONE |
| P1-DATA-Stats-001 | 第一统计源（OP.GG MCP） | DONE |
| P1-DATA-Stats-002 | 第二统计源 | **搁置**（原 LoLChess；DataTFT 未启动） |
| P1-DATA-EntityResolve-001 | Canonical Entity Resolver | DONE |
| P1-DATA-Normalize-001 | Knowledge Normalizer + JDBC store | DONE |
| P1-DATA-Patch-001/002 | Patch Manager / Diff | DONE |
| P1-DATA-Evidence-001 | Evidence Store（InMemory/JDBC） | DONE |
| P1-DATA-Conflict-001 | Conflict Queue（InMemory/JDBC） | DONE |
| P1-DATA-Quality-001 | Source Quality Scoring | DONE |
| P1-KNOW-* | 8 Tools + Catalog JSON | DONE |
| P1-RAG-* | Hybrid RAG + ES dense_vector/BM25 | DONE |
| P1-LLM-* | OpenAI-compatible Gateway | DONE |
| P1-AGENT-* | Knowledge + Research（Tavily/SerpAPI） | DONE |

## 快速启动（HTTP）

```powershell
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -pl tft-orchestrator -am spring-boot:run
```

```
GET http://localhost:8080/api/v1/knowledge/ask?question=What%20does%20interest%20gold%20look%20like%20at%2050%20gold%3F
GET http://localhost:8080/api/v1/research/ask?topic=set17%20meta%20trend
```

默认 `patch=set17-16.16`。`tft.platform.mode=auto` 时若 `.env` 含 MySQL+ES 则走在线栈。

## 存储

- **CI/离线**：InMemory Evidence/Conflict/Canonical/Patch + Bm25 + Hash Embedding + Stub LLM/Search
- **在线**：MySQL（结构化）+ Elasticsearch（BM25 + dense_vector）+ 智谱 LLM/Embedding
- Schema：`tft-data/src/main/resources/db/mysql/V1__p1_stores.sql`
- 评测集：`tft-knowledge/src/main/resources/knowledge/eval/qa_100.json`

## 搁置项

- **P1-DATA-Stats-002**：第二统计源本轮不做（可后续接 DataTFT）
