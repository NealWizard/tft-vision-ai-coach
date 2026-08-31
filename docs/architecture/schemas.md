# Schema 契约

契约采用 **Schema 先行**：先改 `schemas/`，再写 Java 代码与样例。

## 目录

| 路径 | 说明 |
|------|------|
| `schemas/canonical/` | 领域 Canonical 模型（GameState、Champion 等） |
| `schemas/agent/` | Agent 输入输出 Contract |
| `schemas/agent/samples/` | 5 个 Agent 样例 JSON |
| `tft-contracts/` | Schema 打包与校验模块 |

## Canonical Schema（P0-FOUND-Contract-001）

| 文件 | 用途 |
|------|------|
| `common-defs.schema.json` | 公共类型定义 |
| `champion.schema.json` | 棋子 |
| `trait.schema.json` | 羁绊 |
| `item.schema.json` | 装备 |
| `augment.schema.json` | 强化符文 |
| `mechanic.schema.json` | 赛季机制 |
| `patch.schema.json` | 版本补丁 |
| `rule.schema.json` | 规则条目 |
| `observation.schema.json` | 观测事件 |
| `evidence.schema.json` | 证据链 |
| `gamestate.schema.json` | 对局状态聚合 |
| `candidate-set.schema.json` | P3 决策输出（2～3 候选） |
| `contest-snapshot.schema.json` | 争抢快照 |
| `projected-state.schema.json` | 经济/商店投影 |

源码：[schemas/canonical/](https://github.com/NealWizard/tft-vision-ai-coach/tree/develop/schemas/canonical)

## Agent Contract（P0-FOUND-AgentContract-001）

- 主 Schema：`schemas/agent/agent-contract.schema.json`
- 样例：`composition-agent`、`shop-agent`、`economy-agent`、`meta-agent`、`knowledge-agent`

!!! important "输出约束"
    Domain Agent **不得**输出单一强制动作；必须输出 `candidates[]` 供玩家选择。

## 校验流程

```powershell
# 构建时会打包 schema 到 tft-contracts
mvn -pl tft-contracts -am test
```

新增 Schema 时：

1. 在 `schemas/` 添加或修改 JSON Schema
2. 同步到 `tft-contracts/src/main/resources/schemas/`（或通过构建复制）
3. 补充/更新 `schemas/agent/samples/` 样例
4. 在本 Wiki 补充说明（如有业务语义变化）

版本规则见根目录 `schemas/README.md`；测试会校验源码与打包副本完全一致。
