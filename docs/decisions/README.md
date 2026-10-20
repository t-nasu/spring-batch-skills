# 意思決定の記録

スキルの決まりを定めた経緯と理由。スキルとともには配布しない（`scripts/install.sh` は `skills/` のみをコピーする）。

| 番号 | 決定 | 日付 |
|---|---|---|
| [0001](0001-skill-scope.md) | スキルはチーム固有の決まりだけを扱う | 2026-10-19 |
| [0002](0002-batch-vs-boot.md) | 迷ったら素の Spring Boot、Batch は条件に当てはまる時のみ | 2026-10-19 |
| [0003](0003-chunk-reader-mapper.md) | chunk の Reader の作り方は開発者が選ぶ | 2026-10-19 |
| [0004](0004-naming-and-sql.md) | `UseCase` の命名、SQL は XML、最上位パッケージは `batch` | 2026-10-19 |
| [0005](0005-versions.md) | 版はスキル内の表で指定し、社内のリポジトリは使わない | 2026-10-19 |
| [0006](0006-in-house-skills.md) | 社内スキルは名で示して従わせ、`UseCase` の命名などは本スキルを優先 | 2026-10-19 |
| [0007](0007-implementation-plan.md) | 実装計画を設計図とは別に `docs/batch-specs/plans/` へ残す | 2026-10-19 |
| [0008](0008-architecture-spec.md) | ArchitectureSpec（ArchUnit）で層構成と domain の決まりを検証する | 2026-10-19 |
| [0009](0009-metrics-notification.md) | 異常時の通知は Prometheus のカウンタで行う | 2026-10-20 |

## 未決

- DB マイグレーションに Flyway を使うか
- テストに Testcontainers を使うか
