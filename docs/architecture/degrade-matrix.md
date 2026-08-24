# 本地优先与云端降级矩阵

> Roadmap：`P0-FOUND-Degrade-001`

核心能力在无云服务、无 API Key 或云端故障时仍须启动。路由结果由
`com.tft.coach.common.degrade.DegradeRouter` 统一给出并记录降级原因。

| Provider | 正常路径 | 首选降级 | 最终确定性降级 |
|----------|----------|----------|------------------|
| LLM | Cloud LLM | Tool-only | 结构化模板回答 |
| Embedding | Cloud Embedding | 本地 Embedding | 结构化/关键词检索 |
| Reranker | Cloud/Local Reranker | 原始 Hybrid 排序 | BM25/Vector 原分数 |
| Vision | Cloud Vision | 本地 OCR/CV | 人工确认或跳过低置信度字段 |

## 约束

1. P0 不接入业务 LLM/RAG，只定义路由和降级契约。
2. 云端禁用或不可用时返回 `degraded=true` 和稳定原因码。
3. Provider 实现不得绕过该矩阵直接调用云服务。
4. Live 能力仍由独立 Feature Flag 控制，默认关闭。
