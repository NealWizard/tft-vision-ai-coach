# P2 收尾（C–E，跳过实机 ROI 校准）

> 日期：2026-08-31  
> 决策：截图校准延后；用 **Observation fixture** 打通剩余 P2，使 CI 可验收  
> 不做：实机 97%、ONNX 图标、默认 Cloud Vision、Live

## 范围

| 任务 | 本批交付 | Done |
|------|----------|------|
| OCR-002 | 商店 5 槽占位 ROI；Builder 读 `shop.{i}.champion_id/cost` | fixture 五卡顺序/费用正确 |
| Board / Entity | Builder 读 board/bench/trait/item/augment/mechanic 字段 | fixture 可还原 |
| Fallback | `CloudVisionFallback.shouldCallCloud`；默认关闭 | 关闭时主链不调用云 |
| Benchmark | 期望 map vs Observation 列表 → accuracy / low_confidence | 报告可序列化 |
| Builder | Observation → GameState + schema 校验 | Player 必填 |
| Fusion | 同 field 取更高 confidence，平手取更新；`<0.80` 丢弃 | 可解释 |
| Diff | 两帧 Player/Shop/HP 变化 → 事件 | Buy/Gold/HP 等 |
| Timeline | 有序 GameState + 相邻 Diff | 连续可重建 |

## 字段约定

`stage`、`player.{level,gold,hp,xp,streak}`、`shop.{0-4}.{champion_id,cost}`、`board.{i}.{champion_id,star,row,col}`、`bench.{i}.champion_id`、`item.{i}`、`trait.{i}.{id,count}`、`augment.{i}`、`mechanic.{i}`
