# order-import-batch (orderImportJob)

取引先から届く前日の注文 CSV を取り込み、取り込めたら業務日付の日次売上を集計する
バッチ。仕様は [`docs/batch-specs/t3-order-import.md`](docs/batch-specs/t3-order-import.md)。

| 項目 | 内容 |
|---|---|
| 言語・ビルド | Java 21 / Gradle 8.14 |
| フレームワーク | Spring Boot 4.1 / Spring Batch 6.0 |
| DB アクセス | MyBatis 3.5（mybatis-spring-boot-starter 4.1） |
| DB | MySQL 8.0（テストは H2 の MySQL 互換モード） |
| 構成 | レイヤードアーキテクチャ + DDD |

---

## 1. 起動方法

```bash
java -jar build/libs/order-import-batch.jar \
     --targetDate=20260118 \
     --inputFile=/data/in/orders_20260118.csv
```

`--name=value` 形式と `name=value` 形式のどちらでも受け取る。

| 引数 | 必須 | 内容 |
|---|---|---|
| `targetDate` | ○ | 業務日付（`yyyyMMdd`）。取り込んだ注文の帰属日であり、日次売上の集計対象日 |
| `inputFile` | ○ | 取り込む CSV のパス |

### 終了コード

ジョブスケジューラ側で再実行の要否を判断できるよう、失敗の種類ごとに値を分けている。

| 値 | 意味 | 運用の判断 |
|---|---|---|
| 0 | 正常終了 | — |
| 1 | 取り込みまたは集計が失敗。運用チームへ通知済み | 原因を直して**同じ引数で再実行**（失敗箇所から再開する） |
| 2 | 同じジョブが既に実行中。何も処理していない | 先行実行の終了を待つ |
| 3 | ジョブ引数が不正。何も処理していない | 引数を直して再実行 |
| 4 | その業務日付は既に正常終了済み。何も処理していない | 誤起動。再取り込みが必要ならデータを整理してから |

---

## 2. 事前準備（DB スキーマ）

バッチは DDL を自動実行しない（`spring.sql.init.mode: never`）。初回および
バージョンアップ時に次の順で適用する。

```bash
mysql -u root order_import < src/main/resources/db/mysql/01-spring-batch-metadata.sql
mysql -u root order_import < src/main/resources/db/mysql/02-application-schema.sql
```

- `01-…` は Spring Batch 6 のメタデータ表（`spring-batch-core` の `schema-mysql.sql` と同一）。
  **再開機能はこの表に依存する。**
- `02-…` は業務表（`customers` / `products` / `orders` / `daily_sales` / `batch_job_lock`）。
  H2 の MySQL 互換モードでもそのまま流せる構文に揃えてあり、テストは本番と同じこの
  ファイルを使う（DDL の二重管理を避けるため）。

接続先は環境変数で指定する。

| 環境変数 | 既定値 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/order_import?...` |
| `DB_USER` | `order_import` |
| `DB_PASSWORD` | （空） |

---

## 3. 処理の流れ

```
OrderImportJobRunner            二重起動ロックを獲得 → ジョブ起動 → ロック解放 → 終了コード
  └ orderImportJob
      ├ importOrdersStep          チャンク指向（既定 1,000 件 / トランザクション）
      │    CSV 読み取り → 検証 → 注文表へ登録
      └ aggregateDailySalesStep   Tasklet（取り込みが成功したときだけ実行）
           業務日付の注文を集計 → 日次売上表へ登録
```

### 検証

1 行につき、通るべき条件は次のとおり。1 行に複数の誤りがあっても最初の 1 件で
打ち切らず、すべて集めてから報告する。

- 各項目が書式として妥当（必須・桁数・数値・日時）
- 数量が 1 以上
- 顧客コードが顧客マスタに実在する
- 商品コードが商品マスタに実在する

検証を通らなかった行と、列数が合わず行として解釈できなかった行は読み飛ばす。
読み飛ばしは 1 件ずつ行番号と理由を WARN ログに残す。**上限 100 件**
（`batch.order-import.skip-limit`）を超えた時点でジョブは異常終了する。

一時的なエラーの再試行は仕様により行わない（リトライ未設定）。

### 日次売上の粒度

`daily_sales` は **「業務日付 × 商品」** を主キーとする。仕様書に粒度の指定が
なかったため、注文が持つ情報のうち集計軸になりうる商品で分けている。合計だけが
必要なら、この表を業務日付で合算すれば得られる。

集計対象は**注文日時ではなく業務日付**で決まる。`orders.business_date` には
ジョブ引数の `targetDate` をそのまま保持しており、ファイルの締め時刻がずれても
集計対象が変わらない。

### 通知

異常終了時に `NotificationGateway` 経由で運用チームへ通知する。仕様どおり
**実装は仮**で、[`LoggingNotificationGateway`](src/main/java/com/example/orderimport/infrastructure/notification/LoggingNotificationGateway.java)
が本文をログに出すだけ。メールに差し替えるときはこのクラスを置き換えれば足り、
アプリケーション層には手を入れない。

通知するのは「取り込みまたは集計の失敗」だけで、二重起動（終了コード 2）と
引数不正（終了コード 3）は通知しない。これらは処理の失敗ではなく起動手順の
問題であり、終了コードとログで判別できるため。

---

## 4. 異常時の運用

### 失敗した所から再開する

**同じ `targetDate` でもう一度起動するだけ**でよい。

```bash
java -jar order-import-batch.jar --targetDate=20260118 --inputFile=/data/in/orders_20260118.csv
```

Spring Batch が前回の実行を引き継ぎ、

- 完了済みのステップは読み飛ばす
- 失敗したステップは**最後にコミットされたチャンクの続き**から読み直す
- 登録済みの注文は読み直さないので、二重登録は起きない
- 集計ステップは対象業務日付を削除してから入れ直すので、何度走っても二重計上しない

という挙動になる。`inputFile` は非識別引数なので、ファイルを別の場所に移していても
同じ仕事の続きとして扱われる。**中身は同じファイルを指すこと**（読み取り位置を
行数で覚えているため、別内容のファイルを渡すとずれる）。

> 注意: 読み飛ばし件数の上限はステップ実行ごとに数え直される。再開後は再び 0 件から
> 100 件まで読み飛ばせる。

### プロセスが強制終了したとき

- **二重起動ロック**（`batch_job_lock`）は、獲得から
  `batch.order-import.lock-stale-after`（既定 6 時間）を超えると次回起動時に
  自動で回収される。起動 4:00・期限 7:00 という運用に対して十分長く、翌日の
  起動までには必ず切れる長さにしてある。すぐに動かしたいときは手で消してよい。

  ```sql
  DELETE FROM batch_job_lock WHERE job_name = 'orderImportJob';
  ```

- **ジョブ実行が `STARTED` のまま残る**と、Spring Batch は再開を拒否する
  （終了コード 1 でその旨をログに出す）。`BATCH_JOB_EXECUTION` を確認し、
  実際にプロセスが生きていないことを確かめたうえで停止扱いにしてから再実行する。

### 入力ファイルの書式が変わったとき

ヘッダ行の列構成が想定と違うと、1 行も取り込まずに異常終了する。列が入れ替わった
ことに気づかずに取り込むと、顧客コードと商品コードが入れ替わったまま検証を
すり抜けてしまうため、意図的に即座に止めている。取引先に確認すること。

---

## 5. 設定

`application.yml` の `batch.order-import` 配下。

| キー | 既定値 | 内容 |
|---|---|---|
| `chunk-size` | 1000 | 1 トランザクションで登録する件数。再開時はこの単位まで巻き戻る |
| `skip-limit` | 100 | 読み飛ばせる不正データの上限 |
| `encoding` | UTF-8 | 入力ファイルの文字コード |
| `ordered-at-format` | `yyyy-MM-dd HH:mm:ss` | CSV の注文日時の書式 |
| `lock-stale-after` | PT6H | この時間を超えたロックは異常終了の残骸として引き継ぐ |
| `runner.enabled` | true | false にすると起動時にジョブを実行しない（テスト・組み込み用） |

---

## 6. パッケージ構成

```
com.example.orderimport
├── presentation/          起動・ジョブ定義（Spring Batch に触れてよいのはここだけ）
│   ├── runner/              コマンドライン引数の解釈、ロック獲得、終了コード
│   ├── job/                 ジョブ組み立て、ジョブ引数の名前と検証
│   ├── step/                ステップ定義、リーダ／プロセッサ／ライタ、Tasklet
│   └── listener/            異常終了通知、読み飛ばしの記録
├── application/           ユースケース
│   ├── order/               1 行の検証と注文への組み立て、注文の登録
│   ├── sales/               日次売上の集計
│   ├── lock/                二重起動ロック
│   ├── notification/        異常終了の通知文面
│   └── port/                外部へ出ていく口（NotificationGateway / JobLockStore）
├── domain/                業務の語彙と規則（他のどの層にも依存しない）
│   ├── model/               Order / DailySales / BusinessDate と値オブジェクト
│   ├── repository/          リポジトリのインタフェース
│   ├── service/             他集約を参照する検証（マスタ実在チェック）
│   └── validation/          検証エラー
├── infrastructure/        技術的な実現手段
│   ├── datasource/          MyBatis によるリポジトリ実装、マッパー、行 DTO
│   ├── file/                CSV の書式・解釈
│   └── notification/        通知の仮実装
└── config/                横断的な設定
```

依存の向きは `presentation → application → domain`、`infrastructure → domain`。
ドメイン層は Spring Batch も MyBatis も知らない。

### 設計上の判断

- **検証は読み取り時ではなく処理時に行う。** CSV リーダは文字列のまま詰め替える
  だけにしてある。リーダ側で型変換すると、変換失敗が「読み取りエラー」として
  検証エラーとは別経路で扱われ、不正データの数え方と報告が二重化するため。
- **集計は SQL の `GROUP BY` に任せる。** 最大 30 万件をアプリ側に読み出して畳むのは
  現実的でない。リポジトリは畳んだあとの行だけを返す。
- **マスタの存在チェックはプロセス内にキャッシュする。** 1 行ごとに問い合わせると
  最大 60 万回の往復になる。`CachingCustomerRepository` / `CachingProductRepository`
  がリポジトリの実装を包み、同じコードの再照会を省く。
- **ジョブリポジトリは明示的に JDBC 版を使う**（[`BatchInfrastructureConfig`](src/main/java/com/example/orderimport/config/BatchInfrastructureConfig.java)）。
  Spring Batch 6 の既定は何も永続化しない `ResourcelessJobRepository` で、
  Spring Boot の自動設定もそれをそのまま採用する。放っておくとジョブは普通に
  動くのに再開だけが成立しないため、この設定は外さないこと。

---

## 7. ビルドとテスト

```bash
./gradlew build          # コンパイル + テスト + 実行可能 jar
./gradlew test           # テストのみ
```

テストは Docker も MySQL サーバも必要としない。H2 を MySQL 互換モードで起動し、
**本番と同じ DDL** を流して動かす。

主な検証内容:

| テスト | 確かめていること |
|---|---|
| `OrderImportJobIntegrationTest` | 正常系、不正データの読み飛ばし、上限 100 件ちょうど／超過、ヘッダ不一致、引数検証 |
| `OrderImportJobRestartIntegrationTest` | 途中で落ちてから再実行したとき、続きから再開して二重登録しないこと |
| `OrderImportJobRunnerIntegrationTest` | 終了コードの出し分け、二重起動時に 1 件も取り込まないこと、引数の打ち間違いがその業務日付を詰まらせないこと |
| `JobExecutionLockIntegrationTest` | 二重起動の阻止、他プロセスのロックを壊さないこと、古いロックの回収 |
| `DailySalesAggregationIntegrationTest` | 集計の正しさと冪等性（再開後に走っても二重計上しない） |
| `OrderImportJobVolumeTest` | チャンクをまたぐ件数でも件数・金額が合うこと |
| `OrderLineImportServiceTest` ほか | 検証規則と値オブジェクトの不変条件 |

### 残課題

- **メール送信は未実装。** `NotificationGateway` の口だけ用意してあり、現状は
  ログ出力のみ（仕様の「実装は仮でよい」に従う）。送信基盤が決まり次第差し替える。
- **本番相当の性能試験は別途必要。** 上記の量テストは H2 上の 5,000 件規模であり、
  30 万件を 4:00〜7:00 に収められるかは MySQL での実測で確認すること。
