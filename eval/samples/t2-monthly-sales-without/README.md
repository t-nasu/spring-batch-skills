# monthly-sales-batch

月次売上集計バッチ（`monthlySalesSummaryJob`）。前月の取引明細から、店舗別・商品別の月次売上集計を作る。

仕様: [docs/batch-specs/t2-monthly-sales.md](docs/batch-specs/t2-monthly-sales.md)
設計・運用の詳細: [docs/monthly-sales-batch.md](docs/monthly-sales-batch.md)

## 構成

| | |
|---|---|
| 言語・実行環境 | Java 21 |
| ビルド | Gradle 8.14（Wrapper 同梱） |
| フレームワーク | Spring Boot 4.1 / Spring Batch 6.0 |
| DB アクセス | MyBatis 3.5（mybatis-spring-boot-starter 4.1） |
| DB | MySQL 8.x（テストは H2 の MySQL 互換モード） |
| アーキテクチャ | レイヤードアーキテクチャ + DDD |

## ビルドとテスト

```bash
./gradlew build        # コンパイル + テスト + 実行可能 JAR
./gradlew test         # テストのみ
```

テストは H2（インメモリ）で完結するので、DB を用意しなくても実行できる。

## 実行

```bash
java -jar build/libs/monthly-sales-batch.jar targetMonth=202509
```

- `targetMonth=yyyyMM` は **オプション引数ではない**（`--` を付けない）。Spring Boot がこの形式の引数をジョブパラメータに変換する。
- 接続先は環境変数 `DB_URL` / `DB_USER` / `DB_PASSWORD` で指定する。
- 終了コード: 正常終了 0、ジョブ異常終了 5、起動失敗（引数不正・二重起動など）1。

DB の準備（表の作成）は [docs/monthly-sales-batch.md](docs/monthly-sales-batch.md#db-の準備) を参照。

## パッケージ構成

```
com.example.salesbatch
├── domain          ドメイン層: 値オブジェクト・集約・ドメインサービス・リポジトリのポート
│   ├── model       TargetMonth / SalesTransaction / MonthlySalesSummary など
│   ├── service     MonthlySalesAggregation（売上の加算・返品の減算）
│   ├── repository  SalesTransactionRepository / MonthlySalesSummaryRepository（インタフェース）
│   └── exception   InvalidSalesTransactionException
├── application     アプリケーション層: ユースケースの組み立て
│   ├── service     MonthlySalesGroupLoader（読み込みと集計の進行）/ InvalidTransactionSkipPolicy ほか
│   ├── port        OperationNotifier（運用チームへの通知ポート）
│   └── exception   SkipLimitExceededException
├── infrastructure  インフラ層: MyBatis による永続化、通知の実装
│   ├── mybatis     マッパーインタフェースとレコード
│   ├── repository  ドメインのリポジトリポートの実装
│   └── notification
├── presentation    プレゼンテーション層: Spring Batch のジョブ定義
│   └── batch       ジョブ・ステップ・Reader/Writer/Tasklet・リスナー・リトライポリシー
└── config          Bean の組み立てと設定値
```

依存の向きは `presentation → application → domain`、`infrastructure → domain`（ポートの実装）。
ドメイン層とアプリケーション層は Spring に依存しない（Bean 登録は `config` で行う）。
