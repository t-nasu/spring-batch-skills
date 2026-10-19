# monthly-sales-batch

前月の取引明細から、店舗別・商品別の月次売上集計を作るバッチ。

- 仕様書: [docs/batch-specs/t2-monthly-sales.md](docs/batch-specs/t2-monthly-sales.md)
- 設計図: [docs/batch-specs/monthlySalesSummaryJob.design.md](docs/batch-specs/monthlySalesSummaryJob.design.md)

## 使う版

| 対象 | 版 |
|---|---|
| Java | 25（Gradle の toolchain で指定） |
| Gradle | 9.8.1（Wrapper） |
| Spring Boot | 4.1.1 |
| Spring Batch | 6.0.5（Spring Boot の BOM による） |
| MySQL Connector/J | 9.7.0（Spring Boot の BOM による） |
| MyBatis Spring Boot Starter | 4.1.0 |

## 組み立て

```
./gradlew build
```

JDK 25 が手元に無い場合、Gradle の toolchain 自動取得が働く（`settings.gradle` の
foojay resolver）。取得できない閉じた環境では、JDK 25 を入れたうえで
`org.gradle.java.installations.paths` に場所を渡す。

## 起動

```
java -jar build/libs/monthly-sales-batch-1.0.0.jar targetMonth=202509
```

`targetMonth` は `yyyyMM`。オプション引数（`--targetMonth=...`）ではなく、
この形で渡すとジョブパラメータになる。

| 終了コード | 意味 |
|---|---|
| 0 | 正常終了 |
| 0 以外 | 異常終了。運用チームへ `OPS_ALERT` ロガーで通知済み |

### 二重起動と再開

- `targetMonth` は識別用ジョブパラメータなので、同じ年月で完了済みのジョブは Spring Batch が拒否する。
  実行中のものがあれば、二重起動も防がれる。
- 途中で失敗した場合は、**同じ引数でもう一度起動する**と、中断した所から再開する。
  集計は chunk 単位のトランザクションで加算されており、失敗した chunk はロールバック済みなので、
  再開しても二重計上にはならない。
- 読み出し順による再開を前提にしているため、このステップを並列化してはならない。

## DB

接続先は環境変数で渡す。

| 変数 | 既定値 |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `sales` |
| `DB_USER` | `sales` |
| `DB_PASSWORD` | （空） |

業務表の DDL は [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql)。
Spring Batch の管理表は `spring-batch-core` の中の `schema-mysql.sql` を使って別途作る
（アプリは `spring.batch.jdbc.initialize-schema=never` で、自動では作らない）。

## 設定

`application.yml` の `batch.monthly-sales` で変えられる。

| 項目 | 既定値 | 意味 |
|---|---|---|
| `chunk-size` | 1000 | 一度にまとめて処理・コミットする取引の件数 |
| `skip-limit` | 1000 | 飛ばしてよい不正データの上限件数 |
| `deadlock-retry-limit` | 3 | デッドロック時の再試行回数（最初の試行を除く） |

## 層構成

```
batch → application → domain ← infrastructure
```

`domain` は Spring・MyBatis・Spring Batch の型を取り込まない。
これは `DomainLayerDependencyTest` が機械的に確かめている。
