# 比較で作らせたコード

2026-10-19 に Claude Code（Opus 5）で作らせたもの。結果の要約は [../results.md](../results.md)。手を加えていない（`build/` などの生成物とスキルのコピーのみ除いた）。

| 課題 | スキルあり | スキルなし |
|---|---|---|
| t1 ポイント失効 | [t1-expire-points-with](t1-expire-points-with) | [t1-expire-points-without](t1-expire-points-without) |
| t2 月次売上 | [t2-monthly-sales-with](t2-monthly-sales-with) | [t2-monthly-sales-without](t2-monthly-sales-without) |
| t3 注文取込 | [t3-order-import-with](t3-order-import-with) | [t3-order-import-without](t3-order-import-without) |

スキルありの設計図は各ディレクトリの `docs/batch-specs/*.design.md` にある。

ビルドには JDK 25 が要る（スキルなしの3本は JDK 21）。
