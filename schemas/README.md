# Schema 版本治理

Roadmap 任务：`P0-FOUND-Contract-001`、`P0-FOUND-AgentContract-001`。

## 规则

1. `schema_version` 使用语义化版本；破坏兼容的字段变化提升主版本。
2. Canonical ID 使用 `{type}.{slug}`，例如 `champ.ahri`；来源原始 ID 作为映射别名保存。
3. Schema 源文件位于本目录，并同步到 `tft-contracts/src/main/resources/schemas/`。
4. 修改 Schema 时必须同步更新样例和 `SchemaContractTest`；CI 会校验两份 Schema 完全一致。
5. 已发布版本不得原地改变语义；需要不兼容变更时新增版本并提供迁移说明。

`confidence` 作为 `canonical/common-defs.schema.json` 中的公共定义维护，不单独发布重复 Schema。
