# P1 数据层

> 更新时间：2026-08-22

## 模块

| 模块 | 职责 |
|------|------|
| `tft-data` | Source Adapter SPI、Raw Snapshot、外部源抓取 |
| `tft-knowledge` | 归一化、Patch、Tools、RAG、Knowledge Agent |

## Source Adapter SPI（P1-DATA-SourceAdapter-001）

```
com.tft.coach.data.spi.SourceAdapter
com.tft.coach.data.registry.SourceAdapterRegistry
com.tft.coach.data.fetch.SourceFetchService
```

- 所有 Adapter 实现统一 `fetch(FetchRequest)` 接口
- `SourceFetchService`：live 成功 → 追加快照；失败 → 返回最近可用 Snapshot（`degraded=true`）
- `SourceType` 与 `evidence.schema.json` 的 `source_type` 对齐

## Raw Snapshot（P1-DATA-Snapshot-001）

```
com.tft.coach.data.snapshot.RawSnapshotStore
com.tft.coach.data.snapshot.FileSystemRawSnapshotStore
```

- 默认存储路径：`data/snapshots/`（gitignore，本地运行时）
- **append-only**：每次抓取新 UUID 目录，不覆盖历史
- 可按 `sourceType + sourceId + resourceKey + 时间窗` 查询
- 元数据包含 SHA-256 checksum、采集时间和本地写入时间

## P1 任务进度

| ID | 任务 | 状态 |
|----|------|------|
| P1-DATA-SourceAdapter-001 | Source Adapter SPI | V3.1 重验收 |
| P1-DATA-Snapshot-001 | Raw Snapshot Store | V3.1 重验收 |
| P1-DATA-Riot-001 | Riot/Data Dragon Adapter | V3.1 重验收 |
| P1-DATA-Stats-001 | 第一统计源 Adapter | V3.1 重验收 |
| P1-DATA-Stats-002 | 第二统计源 Adapter | V3.1 重验收 |
| P1-DATA-EntityResolve-001 | Canonical Entity Resolver | TODO |
| … | 见 Roadmap V3.1 | |

## Data Dragon Adapter（P1-DATA-Riot-001）

```
com.tft.coach.data.datadragon.DataDragonAdapter
com.tft.coach.data.datadragon.DataDragonFetchService
com.tft.coach.data.evidence.FetchEvidence
```

| 资源 | resourceKey | CDN 文件 |
|------|-------------|----------|
| 棋子 | `tft-champion` | `tft-champion.json` |
| 羁绊 | `tft-trait` | `tft-trait.json` |
| 装备 | `tft-item` | `tft-item.json` |
| 强化 | `tft-augments` | `tft-augments.json` |

- 基址：`https://ddragon.leagueoflegends.com`
- Patch：支持完整版本（`16.16.1`）或前缀（`14.23` → `14.23.1`）；空则取最新
- 失败降级：经 `SourceFetchService` 回退至最近 Snapshot
- Evidence：`FetchEvidence.fromFetchResult()` 对齐 `evidence.schema.json`

示例：

```java
DataDragonFetchService ddragon = DataDragonFetchService.createDefault(
        Path.of("data/snapshots"), new JdkDataDragonHttpClient());
var outcome = ddragon.fetch(DataDragonResource.CHAMPION, "16.16.1", "en_US");
FetchEvidence evidence = outcome.evidence();
```

## OP.GG Stats Adapter（P1-DATA-Stats-001）

```
com.tft.coach.data.meta.MetaSnapshot
com.tft.coach.data.opgg.OpGgMcpStatsAdapter
com.tft.coach.data.opgg.OpGgStatsFetchService
com.tft.coach.data.opgg.OfficialOpGgMcpClient
```

**MetaSnapshot** 统一 DTO，覆盖 Comp / Unit / Item / Augment，含 `sample_size` 与 `captured_at`。

- 第一统计源采用 **OP.GG 官方 MCP**（`https://mcp-api.op.gg/mcp`，工具 `tft_list_meta_decks`）
- `source_id=opgg`，参数：`region`、`time_window`、`patch`
- MCP 原始响应经 `OpGgMcpMetaBundleNormalizer` 归一化后 append-only 存入 Snapshot；live 失败降级缓存
- 解析：`OpGgMetaSnapshotParser`

```java
try (OpGgMcpClient mcp = new OfficialOpGgMcpClient()) {
    OpGgStatsFetchService opgg = OpGgStatsFetchService.createDefault(
            Path.of("data/snapshots"), mcp);
    var outcome = opgg.fetchMetaBundle("set17-16.16", "global", "24h");
    MetaSnapshot meta = outcome.snapshot();
}
```

## LoLChess Stats Adapter + 多源聚合（P1-DATA-Stats-002）

```
com.tft.coach.data.lolchess.LoLChessStatsAdapter
com.tft.coach.data.meta.MultiSourceMetaFetchService
com.tft.coach.data.meta.MetaSnapshotQuery
```

- 第二统计源：`source_id=lolchess`，URL 模板与 OP.GG 对齐（`/api/meta/bundle`）
- 解析复用 `OpGgMetaSnapshotParser`（规范化 JSON，`source_id` 区分厂商）
- **MultiSourceMetaFetchService**：同一 `patch + region + time_window` 拉取 OP.GG + LoLChess，各自保留 `source_id` 与 `FetchEvidence`
- 单源失败不阻塞另一源；经 `SourceFetchService` 可独立降级至缓存

```java
try (OpGgMcpClient mcp = new OfficialOpGgMcpClient()) {
    MultiSourceMetaFetchService meta = MultiSourceMetaFetchService.createDefault(
            Path.of("data/snapshots"),
            mcp,
            new JdkLoLChessStatsHttpClient());
    MultiSourceMetaResult result = meta.fetch(MetaSnapshotQuery.of("set17-16.16", "global", "24h"));
}
```

## 下一步

**P1-DATA-EntityResolve-001**：Canonical Entity Resolver（Data Dragon + 统计源 ID 对齐）。
