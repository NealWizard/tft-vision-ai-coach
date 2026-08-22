# Branching Strategy (P0-001)

## Branches

| Branch | Purpose | Protection |
|--------|---------|------------|
| `main` | Production-ready releases only | PR required; CI must be green |
| `develop` | Integration branch for V0.x | PR required; CI must be green |
| `feature/<id>-<slug>` | Single task/story work | Short-lived; rebase onto develop |
| `hotfix/<slug>` | Urgent main fixes | Merge to main and back-port to develop |

## Naming examples

- `feature/P0-005-canonical-schemas`
- `feature/P1-003-datadragon-adapter`
- `hotfix/ci-safety-scan-false-positive`

## Merge rules

1. No direct push to `main` / `develop` (configure in GitHub **Branch protection rules**).
2. PR must pass CI: `safety-scan`, `build-test` (see `.github/workflows/ci.yml`).
3. Prefer squash merge for feature branches; keep linear history on `develop`.
4. Remote: `https://github.com/NealWizard/tft-vision-ai-coach`
5. Cross-module changes stay in one PR when they share a single acceptance criterion.

## Mono-repo domains

`tft-data` · `tft-knowledge` · `tft-vision` · `tft-state` · `tft-meta` · `tft-decision` · `tft-replay` · `tft-learning` · `tft-orchestrator`

Shared foundations: `tft-contracts` (schemas) · `tft-common` (flags/observability)
