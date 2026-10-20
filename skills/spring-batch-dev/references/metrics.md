# 異常時の通知（Prometheus のカウンタ）

異常の通知は、Micrometer の Prometheus 用レジストリ（`micrometer-registry-prometheus`）でカウンタを増やすことで行う。メール送信などの通知処理は作らない。誰にどう知らせるかは、Prometheus 側のアラートで決める。

## 依存と設定

```groovy
implementation 'org.springframework.boot:spring-boot-starter-micrometer-metrics'
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.prometheus:prometheus-metrics-exporter-pushgateway'
```

いずれも Spring Boot の BOM が管理するので版を書かない。

バッチはすぐに終了し、Prometheus が取りに来る前にプロセスが消えるため、終了時に Pushgateway へ送る。

```yaml
management:
  prometheus:
    metrics:
      export:
        pushgateway:
          enabled: true
          address: ${PUSHGATEWAY_ADDRESS:localhost:9091}
          job: <ジョブ名>
          shutdown-operation: post
```

テストでは `management.prometheus.metrics.export.pushgateway.enabled=false` にする。

## カウンタ

| 名前（Micrometer） | Prometheus での名前 | タグ | 増やすとき |
|---|---|---|---|
| `batch.job.failures` | `batch_job_failures_total` | `job` | ジョブが異常終了したとき（1回の実行で1） |
| `batch.item.skips` | `batch_item_skips_total` | `job`, `step` | 不正データを飛ばしたとき（1件ごとに1） |

- どちらも、ジョブの開始時に 0 で登録しておく。Pushgateway は最後に送られた値を持ち続けるので、正常に終わった回で 0 に戻すため
- タグに ID や業務の値（会員 ID、日付など）を入れない
- 素の Spring Boot で作る場合、`step` タグには処理の段階名を入れる

## 置き場所

- カウンタを増やすのは batch 層だけとする。domain と application は Micrometer に依存しない
- Spring Batch: `JobExecutionListener`（`org.springframework.batch.core.listener`）の `afterJob` で、終了状態が `FAILED` なら `batch.job.failures` を増やす。`SkipListener` で `batch.item.skips` を増やす
- 素の Spring Boot: `ApplicationRunner` で例外を受けたら `batch.job.failures` を増やし、終了コードを異常にする。飛ばしたデータの件数は UseCase の戻り値で受け取り、`batch.item.skips` を増やす
- 仕様書の「異常時」に通知と書かれていても、通知処理（メールなど）は作らず、このカウンタで代える
