# order-import-batch

取引先から届く前日の注文 CSV を取り込み、取り込めたら日次売上を集計するバッチ（`orderImportJob`）。

- 仕様書: [docs/batch-specs/t3-order-import.md](docs/batch-specs/t3-order-import.md)
- 設計図: [docs/batch-specs/orderImportJob.design.md](docs/batch-specs/orderImportJob.design.md)

## 使う版

| 対象 | 版 |
|---|---|
| Java | 25（Gradle の toolchain で指定） |
| Gradle | 9.8.1（Wrapper） |
| Spring Boot | 4.1.1 |
| Spring Batch | 6.0.5（Spring Boot の BOM 管理） |
| MySQL Connector/J | Spring Boot の BOM 管理 |
| MyBatis Spring Boot Starter | 4.1.0 |

## 前もって要るもの

- JDK 25。手元に無ければ Gradle の toolchain が取得する
- MySQL 8.0.19 以降
- 業務表: [src/main/resources/db/schema.sql](src/main/resources/db/schema.sql)
- Spring Batch のメタデータ表: Spring Batch の配布物に入っている `org/springframework/batch/core/schema-mysql.sql`

## ビルド

```
./gradlew build
```

## 実行

```
DB_URL=jdbc:mysql://db:3306/order_import DB_USER=order_import DB_PASSWORD=**** \
java -jar build/libs/order-import-batch-1.0.0.jar \
     targetDate=20261018 \
     inputFile=/data/in/orders_20261018.csv
```

- 起動引数はどちらも必須。足りなければジョブを動かす前に弾く
- 終了コードが 0 以外なら失敗。スケジューラはこれで成否を判定する
- 同じ `targetDate` / `inputFile` での二重起動は、Spring Batch の JobRepository が拒む

### 失敗したときの再開

同じ起動引数でもう一度実行する。取り込みは読み終えた行の続きから始まり、完了済みのステップはやり直さない。

## 層構成

```
com.example.orderimport
├── batch             Job/Step の定義、起動引数の解釈
├── application       ユースケース、トランザクション境界
├── domain            エンティティ、値オブジェクト、ドメインサービス、リポジトリのインタフェース
└── infrastructure    リポジトリの実装（MyBatis）、CSV 読み取り、通知
```

詳しくは [.claude/skills/spring-batch-dev/references/architecture.md](.claude/skills/spring-batch-dev/references/architecture.md)。
