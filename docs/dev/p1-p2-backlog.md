# P1 / P2 / P3 缺口与待办

> 更新时间：2026-08-31  
> 对照 Roadmap V3.1 与当前代码。能本地补的已补；下列为**做不了或不应假装完成**的项。

## P1

| 项 | 状态 | 原因 | 解阻条件 |
|----|------|------|----------|
| `P1-DATA-Stats-002` 第二统计源 | **BLOCKED** | DataTFT 未接入；原 LoLChess 已弃 | 选定源 + 合法 API/MCP |
| Data Dragon HTTP 灌库 | 接口有，CDN 常失败 | 国内访问 `ddragon.leagueoflegends.com` 连接重置 | 镜像/代理，或本地已下载包 |
| Catalog 经济/商店数值 | 仅改了 patch 键 | 利息/商店人数等沿用既有表，未用 18.1 官方表重算 | 有官方 18.1 商店概率与单位池后再改 JSON |
| Offline bootstrap 实体 | 仅少量 fixture | 未灌库时 Champion 列表不完整 | 灌库成功或扩 catalog JSON |

## P2

| 项 | 状态 | 原因 | 解阻条件 |
|----|------|------|----------|
| `P2-VISION-OCR-001` ≥97% | **TODO** | 链路已通，无标注 1080p 黄金集 | 原生 1920×1080 截图 + 标注 |
| ROI 实机校准 | 占位坐标 | 用户图为 1024×475 裁切/叠加摄像头，不能拉伸猜 ROI | 完整 1080p、无遮挡、含底栏与五卡商店 |
| 标定 UI | 未做 | Batch A 明确只做数据+校验 | 独立小任务 |
| Camera / Desktop `FrameSource` | 仅枚举 | 故意冻 SPI | 需要 Live 实验时再实现 |
| Shop/Board/Entity **实机识别** | fixture 可还原；无图标模型 | 无 ONNX/截图 | 校准 ROI + 图标模型或 OCR 名称 |
| Fusion 时间去抖 | 仅 confidence 门禁 | 无连续视频帧 | 录像回放后再做 N 帧一致 |
| Cloud Vision 真调用 | 只做开关，默认关 | 避免厂商锁定 | 产品确认供应商与预算 |
| 侧车 PyInstaller 打包 / auto-spawn | 未做 | 商用客户端后续 | 发布任务 |
| `/roi/crop` OpenCV | 未做 | Java 已 ImageIO crop | 非必须 |

## P3

| 项 | 状态 | 原因 | 解阻条件 |
|----|------|------|----------|
| 在线 Meta 走 OP.GG MCP 写入 MySQL | **未接线** | `DecisionPlatform` 仍 seed fixture + InMemory；V2 DDL 已有 | Orchestrator online 把 DataSource 交给 Jdbc store |
| Rank/Queue 过滤 | degraded | 现有 Snapshot 无该维 | OP.GG 查询参数可用后补 |
| PatchImpact 实体 diff | degraded | 缺第二 patch 实体目录 | 双 patch Canonical 灌库 |

## 已在本轮补上的小缺口

- Python `NumericNormalizer` 与 Java 对齐：`shop.{i}.cost`
- Wiki 首页 / 模块图 / 快速开始接口与 P2 对齐
- `vision-sidecar/scripts/package.ps1` 占位说明（不真打包）
