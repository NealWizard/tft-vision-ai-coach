# Branching Strategy (P0-001)

## Branches

| Branch | Purpose | Protection |
|--------|---------|------------|
| `main` | Production-ready releases only | MR required; CI must be green |
| `develop` | Integration branch for V0.x | MR required; CI must be green |
| `feature/<id>-<slug>` | Single task/story work | Short-lived; rebase onto develop |
| `hotfix/<slug>` | Urgent main fixes | Merge to main and back-port to develop |

## Naming examples

- `feature/P0-005-canonical-schemas`
- `feature/P1-003-datadragon-adapter`
- `hotfix/ci-safety-scan-false-positive`

## Merge rules

1. No direct push to `main` / `develop` (configure in GitLab Protected Branches).
2. MR must pass: `safety:scan`, `compile`, `unit-test`, `static-check`.
3. Prefer squash merge for feature branches; keep linear history on `develop`.
4. Cross-module changes stay in one MR when they share a single acceptance criterion.

## Mono-repo domains

`tft-data` · `tft-knowledge` · `tft-vision` · `tft-state` · `tft-meta` · `tft-decision` · `tft-replay` · `tft-learning` · `tft-orchestrator`

Shared foundations: `tft-contracts` (schemas) · `tft-common` (flags/observability)
