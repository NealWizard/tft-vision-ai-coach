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
| P1-003 | Riot/Data Dragon Adapter | TODO |
| P1-004 | 第一统计源 Adapter | TODO |
| … | 见 User Story 看板 | |

## 下一步

**P1-003**：实现 `riot-datadragon` Adapter，对接 Data Dragon 静态 JSON。
