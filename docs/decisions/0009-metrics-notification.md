# 0009 異常時の通知は Prometheus のカウンタで行う

## 決定（2026-10-20）

- 異常時の通知は、`micrometer-registry-prometheus` でカウンタを増やすことで行う。メールなどの通知処理は作らない
- カウンタは `batch.job.failures`（ジョブの異常終了）と `batch.item.skips`（不正データのスキップ）の2つ。タグは `job` と `step` のみ
- カウンタを増やすのは batch 層（ジョブのリスナー、`ApplicationRunner`）だけとし、domain と application は Micrometer に依存させない。ArchitectureSpec で検証する

## 届け方

バッチはすぐに終了するため、Prometheus が取りに来る（scrape）前にプロセスが消える。Spring Boot 4.1 の Pushgateway 連携（`management.prometheus.metrics.export.pushgateway.*`、`shutdown-operation: post`）で終了時に送る案を推奨とした。偽の Pushgateway を立てて、`batch_job_failures_total` が終了時に届くことを確かめた。届け方は利用者の確認待ち。

Pushgateway は最後に送られた値を持ち続けるため、カウンタはジョブの開始時に 0 で登録し、正常に終わった回で 0 に戻るようにする。
