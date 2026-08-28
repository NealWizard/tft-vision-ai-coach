# P1 做浅项真实化设计（方案 1）

> 日期：2026-08-26  
> 状态：已批准并执行中（Stats-002 搁置）

## 目标

将 P1「做浅」能力收口为真实可运行路径：MySQL + Elasticsearch + 云 LLM/Embedding + Research 外网检索；OFFLINE/单测保留 InMemory + Stub。

## 明确不做

- `P1-DATA-Stats-002` / DataTFT（本轮搁置）
- Redis 会话、MinerU、Downstream

## 架构

```
.env → EnvFileLoader → KnowledgePlatformConfig
  ├─ offline/auto无库: KnowledgePlatform.createDefault()
  └─ online:
       MySQL(JDBC): Evidence / Conflict / Canonical / Patch
       ES: BM25 + dense_vector (tft_rag_chunks)
       LLM/Embedding: OpenAI-compatible (智谱)
       Research: Tavily | SerpAPI (默认 Tavily)
       Tools: KnowledgeCatalog JSON + Data Dragon ingest
```

## 数据源

| 类型 | 来源 |
|------|------|
| 规则/概率/牌池/机制 | `classpath:knowledge/catalog/*.json` |
| 实体 | Data Dragon ingest（可失败降级 offline fixture） |
| RAG 评测 | `knowledge/eval/qa_100.json`（100 题） |

## 降级

- `tft.platform.mode=offline` 强制内存栈
- `auto`：无 MYSQL_HOST/ES_HOSTS 则 offline
- ES 初始化失败 → InMemory RAG + Hash embedding
- LLM 配置不全 → StubCloudLlmProvider
- 无检索 Key → StubWebSearchProvider

## HTTP

- `GET/POST /api/v1/knowledge/ask`
- `GET/POST /api/v1/research/ask`

## 验收

- 单元测试在 offline 模式通过
- 在线模式连本机 `.env` 的 MySQL/ES/LLM 可启动
- Tools 不依赖 Java 内硬编码事实表（catalog/JSON）
