# P3 Meta & Decision 设计（V1.1）

> 日期：2026-08-31  
> 来源：`P3_Meta_Decision_完善执行计划_V1.1.docx`  
> 状态：执行中

## 目标

给定合法 GameState，系统读取当前 Patch Meta、Knowledge Tools 与 Decision RAG，由 Domain Agent 生成 **2～3** 个可验证候选。Cloud LLM 只写 `reasoning`/`tradeoff`。无云仍输出确定性 CandidateSet + `degraded`。

## 明确不做

P5 Planner/Registry、Live、战斗仿真、ClickHouse/Redis/新 Vector DB、OCR≥97%、改 HTTP 默认 Patch。

## 批次

| 批 | 内容 | LLM |
|----|------|-----|
| A | Meta Snapshot/Score/Trend/PatchImpact + CandidateSet + thin Pipeline + Decision RAG context + HTTP | 无 |
| B | Shop / Economy / Composition + Case RAG | 无 |
| C | Item / Augment / Transition / Risk + 经济/商店 Simulator | 无 |
| D | ChatModelGateway + Grounding + Reasoning | 仅解释 |

## 不变量（摘要）

- INV-001/002：LLM 不改 score、不发明 TFT 数字
- INV-004/005：无 GameState / 无 Patch 不决策
- INV-006：Simulator 只投影 gold/interest/xp/level/shop odds
- INV-007：Domain Agent 不 import 厂商 LLM SDK
- INV-009：A/B/C 无云可跑

## Meta Context Provider

Batch A 的 Meta Agent **只提供 Meta 上下文候选**（`decision_type=COMPOSITION`，`action_type=PIVOT` 指向 top comps），不负责商店买卖。

## 契约文件

- `schemas/canonical/candidate-set.schema.json`
- `schemas/canonical/contest-snapshot.schema.json`
- `schemas/canonical/projected-state.schema.json`

## 依赖

`tft-meta` → tft-data/contracts；`tft-decision` → tft-state/tft-meta/tft-knowledge/contracts。禁止反向依赖。
