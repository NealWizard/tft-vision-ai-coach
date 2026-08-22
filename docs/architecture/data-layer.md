# P1 数据层

> 更新时间：2026-08-22

## 模块

| 模块 | 职责 |
|------|------|
| `tft-data` | Source Adapter SPI、Raw Snapshot、外部源抓取 |
| `tft-knowledge` | 归一化、Patch、Tools、Knowledge Agent（P1-007+） |

## Source Adapter SPI（P1-001）

```
com.tft.coach.data.spi.SourceAdapter
com.tft.coach.data.registry.SourceAdapterRegistry
com.tft.coach.data.fetch.SourceFetchService
```

- 所有 Adapter 实现统一 `fetch(FetchRequest)` 接口
- `SourceFetchService`：live 成功 → 追加快照；失败 → 返回最近可用 Snapshot（`degraded=true`）
- `SourceType` 与 `evidence.schema.json` 的 `source_type` 对齐

## Raw Snapshot（P1-002）

```
com.tft.coach.data.snapshot.RawSnapshotStore
com.tft.coach.data.snapshot.FileSystemRawSnapshotStore
```

- 默认存储路径：`data/snapshots/`（gitignore，本地运行时）
- **append-only**：每次抓取新 UUID 目录，不覆盖历史
- 可按 `sourceType + sourceId + resourceKey + 时间窗` 查询

## P1 任务进度

| ID | 任务 | 状态 |
|----|------|------|
| P1-001 | Source Adapter SPI | DONE |
| P1-002 | Raw Snapshot 存储 | DONE |
| P1-003 | Riot/Data Dragon Adapter | DONE |
| P1-004 | OP.GG 统计源 Adapter | DONE |
| P1-005 | 第二统计源 Adapter | TODO |
| … | 见 User Story 看板 | |

## Data Dragon Adapter（P1-003）

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

## OP.GG Stats Adapter（P1-004，PRD 推荐第一统计源）

```
com.tft.coach.data.meta.MetaSnapshot
com.tft.coach.data.opgg.OpGgStatsAdapter
com.tft.coach.data.opgg.OpGgStatsFetchService
```

**MetaSnapshot** 统一 DTO，覆盖 Comp / Unit / Item / Augment，含 `sample_size` 与 `captured_at`。

- `source_id=opgg`，参数：`region`、`time_window`、`patch`
- 原始 JSON append-only 存入 Snapshot；live 失败降级缓存
- 解析：`OpGgMetaSnapshotParser`

```java
OpGgStatsFetchService opgg = OpGgStatsFetchService.createDefault(
        Path.of("data/snapshots"), new JdkOpGgStatsHttpClient());
var outcome = opgg.fetchMetaBundle("set17-16.16", "global", "24h");
MetaSnapshot meta = outcome.snapshot();
```

## 下一步

**P1-005**：第二统计源 Adapter（lolchess.gg，交叉验证并保留 `source_id`）。
