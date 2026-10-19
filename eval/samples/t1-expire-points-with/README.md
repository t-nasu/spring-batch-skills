# expire-points-batch

有効期限を過ぎた会員ポイントを失効させ、失効履歴を残すバッチ（`expirePointsJob`）。

- 仕様: [docs/batch-specs/t1-expire-points.md](docs/batch-specs/t1-expire-points.md)
- 設計: [docs/batch-specs/expirePointsJob.design.md](docs/batch-specs/expirePointsJob.design.md)

Spring Batch は使わず、素の Spring Boot の `ApplicationRunner` で作っている。根拠は設計書の 1 節にある。

## 使う版

| 対象 | 版 |
|---|---|
| Java | 25 (LTS) — Gradle の toolchain |
| Gradle | 9.8.1 — Gradle Wrapper |
| Spring Boot | 4.1.1 |
| MySQL Connector/J | Spring Boot の BOM に任せる |
| MyBatis Spring Boot Starter | 4.1.0 |

## 層構成

```
com.example.expirepoints
├── batch             ApplicationRunner、起動引数の解釈
├── application       ユースケース（トランザクション境界）
├── domain            エンティティ、値オブジェクト、リポジトリのインタフェース
└── infrastructure    MyBatis の Mapper / XML、リポジトリの実装
```

`domain` は Spring・MyBatis の型を import しない。SQL はすべて `src/main/resources/mapper/` の XML にある。

## 表

`db/schema.sql` を流して作る（アプリからは自動実行しない）。

## ビルド

```sh
./gradlew build
```

日本語のテスト名を使っているため、UTF-8 のロケール（例: `LANG=C.UTF-8`）で実行する。
POSIX ロケールだと Gradle のテストレポート出力が失敗する。

## 実行

```sh
java -jar build/libs/expire-points-batch-1.0.0.jar --targetDate=20261019
```

接続先は環境変数で渡す。

| 変数 | 既定値 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/point?...` |
| `DB_USER` | `point` |
| `DB_PASSWORD` | `point` |

終了コード: `0` = 正常終了、`1` = 異常終了（引数不正、不正データ、DB 障害）。
スケジューラはこの終了コードで成否を判定する。

失敗したときは全件巻き戻っているため、原因を取り除いて同じ引数でやり直せる。
成功済みのポイントは `EXPIRED` になっており再度の対象にならないので、二度実行しても結果は変わらない。
