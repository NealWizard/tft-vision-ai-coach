# Fixtures Versioning Rules (`P0-FOUND-TestData-001`)

## Directory layout

```
fixtures/
  gold_level_round_set/
  shop_set/
  board_set/
  augment_set/
  patch_regression_set/
  knowledge_qa_set/
  video_replay_set/
```

Each dataset contains:

- `manifest.json` — dataset metadata and version
- `samples/` — images and/or JSON sidecars (empty in P0 scaffold)

## Version rules

1. `manifest.version` follows semver for dataset packaging (not game patch).
2. `patch_scope` lists game patches the labels are valid for.
3. Samples are **append-only**; never overwrite a labeled sample — bump seq or create a new dataset version.
4. Sidecar JSON must use Observation / GameState canonical schemas when applicable.
5. Binary assets stay out of Git LFS until P2 dataset collection starts (deferred).
