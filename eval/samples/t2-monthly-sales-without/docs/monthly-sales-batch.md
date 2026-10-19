# 月次売上集計バッチ 設計・運用メモ

対象仕様: [batch-specs/t2-monthly-sales.md](batch-specs/t2-monthly-sales.md)

## 1. ジョブの構成

```
monthlySalesSummaryJob (起動引数: targetMonth=yyyyMM)
├── purgeMonthlySalesSummaryStep  対象年月の既存の集計結果を削除（Tasklet）
└── aggregateMonthlySalesStep     取引明細を店舗 × 商品ごとに集計して登録（チャンク処理）
```

集計ステップのチャンク 1 件は「1 つの集計単位（店舗 × 商品）の集計結果」。
`chunk-size` 件の集計単位をまとめて 1 トランザクションで登録する。

```
 sales_transaction                MonthlySalesGroupLoader              monthly_sales_summary
 (店舗,商品,取引ID 昇順)   ──▶   ページ単位で読み込み                ──▶  チャンクごとに
                                  不正データは読み飛ばし                   まとめて upsert
                                  集計単位が変わったら 1 件確定
```

### 1 件ずつ読んで集計単位ごとに区切る理由

| 方式 | 採否 | 理由 |
|---|---|---|
| SQL の `GROUP BY` で DB 側に集計させる | × | 不正データを「取引 ID 単位で読み飛ばして続行」できない。途中再開もできない |
| 明細をメモリに全件載せて集計 | × | 最大 800 万件。メモリに載せられない |
| **キー順に読み進め、集計単位の切れ目で確定**（採用） | ○ | メモリ使用量は「1 ページ + 集計中の 1 グループ」で一定。確定した集計単位が再開の目印になる |

## 2. 仕様との対応

| 仕様 | 実装 |
|---|---|
| ジョブ名 `monthlySalesSummaryJob` | `MonthlySalesSummaryJobConfig.JOB_NAME` |
| 起動引数 `targetMonth=yyyyMM` | `TargetMonthJobParametersValidator` で形式を検証。値は `TargetMonth`（値オブジェクト） |
| 定時起動（毎月 1 日 2:00） | ジョブスケジューラから `java -jar ... targetMonth=yyyyMM` を起動する前提。アプリ内にスケジューラは持たない（[6](#6-運用) 参照） |
| 二重起動の防止（同一対象年月） | `targetMonth` を識別用ジョブパラメータにしている。正常終了済みの対象年月で起動すると Spring Batch が `JobInstanceAlreadyCompleteException` で拒否する |
| 取引日が対象年月内の明細を抽出 | `transaction_date >= 月初日 AND < 翌月初日`（`SalesTransactionMapper.xml`） |
| 店舗・商品ごとに金額と数量を合計 | `MonthlySalesAggregation`（ドメインサービス） |
| 返品（`type = 'RETURN'`）は差し引き | `TransactionType.sign()` が `-1` を返し、`SalesTransaction.signedAmount()` / `signedQuantity()` に反映 |
| 集計結果を月次売上表に登録 | `MonthlySalesSummaryItemWriter` → `monthly_sales_summary` へ upsert |
| 失敗した所から再開 | [3](#3-途中で失敗したときの再開) |
| 不正データは飛ばして続行、上限 1,000 件、取引 ID をログ | `InvalidTransactionSkipPolicy`（`batch.monthly-sales.skip-limit`） |
| デッドロックのみ 3 回再試行 | `DeadlockRetryPolicy`（`batch.monthly-sales.deadlock-retry-count`） |
| 運用チームへの通知 | `OperationNotifier` ポート + `LoggingOperationNotifier`（`[OPS-ALERT]` の ERROR ログ） |
| 終了期限 6:00 | 超過したら WARN ログ（`batch.monthly-sales.deadline`）。[7](#7-性能の見積もりと調整) も参照 |

## 3. 途中で失敗したときの再開

### しくみ

集計ステップは、チャンクをコミットするのと**同じトランザクションで**進捗を保存する。

- 進捗として保存するもの（`MonthlySalesSummaryItemReader` が `ExecutionContext` に書く）
  - `monthlySales.lastCompletedStoreId` / `monthlySales.lastCompletedProductId`: 登録まで完了した最後の集計単位
  - `monthlySales.skippedCount`: そこまでに読み飛ばした不正データの件数
- 再開時は `WHERE (店舗, 商品) > 保存した集計単位` で読み直す（`SalesTransactionCursor.AfterGroup`）。
  集計単位の途中から再開することはないので、合計値が中途半端になることはない。
- 読み飛ばし件数は「再開時に読み直す範囲より手前のもの」だけを進捗に含めるため、
  再開しても二重に数えない（`MonthlySalesGroupLoader#committedSkippedCount`）。
- 削除ステップは完了済みとして記録されるため再開時には実行されない。
  つまり、再開しても登録済みの集計結果は消えない。

### 再開のしかた（運用）

**同じ起動引数でもう一度起動するだけ**でよい。

```bash
java -jar monthly-sales-batch.jar targetMonth=202509
```

前回が異常終了（FAILED）なら、同じジョブインスタンスの続きとして実行され、
中断した集計単位の次から処理する。前回が正常終了していれば拒否される（二重起動の防止）。

> この再開は Spring Batch のメタデータ表（`BATCH_*`）に依存する。
> `BatchInfrastructureConfig` で `JobRepository` を JDBC 版に差し替えているのはこのため
> （Spring Batch 6 の既定はメモリ上にしか履歴を残さない `ResourcelessJobRepository` で、
> そのままでは再開も二重起動の防止もできない）。

### 最初からやり直したいとき

対象年月の集計を作り直したい場合は、その対象年月のジョブインスタンスを削除してから起動する。

```sql
-- 対象年月のジョブ実行履歴を確認
SELECT i.JOB_INSTANCE_ID, e.JOB_EXECUTION_ID, e.STATUS, e.START_TIME, e.END_TIME
FROM BATCH_JOB_INSTANCE i
JOIN BATCH_JOB_EXECUTION e ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
JOIN BATCH_JOB_EXECUTION_PARAMS p ON p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
WHERE i.JOB_NAME = 'monthlySalesSummaryJob'
  AND p.PARAMETER_NAME = 'targetMonth' AND p.PARAMETER_VALUE = '202509';
```

登録先 `monthly_sales_summary` はキー重複時に上書きする冪等な登録なので、
作り直しても行が重複することはない。

## 4. 不正データの扱い

集計に使えない明細は読み飛ばし、取引 ID を WARN ログに出力する。

```
不正データのため読み飛ばしました: transactionId=6 reason=数量が未設定です skipped=1/1000
```

読み飛ばす条件（`RawSalesTransaction#toSalesTransaction`）:

| 条件 | 理由 |
|---|---|
| 店舗 ID / 商品 ID が空 | 集計単位を決められない |
| 取引区分が `SALE` / `RETURN` 以外 | 加算か減算かを決められない |
| 金額が未設定、または負数 | 返品は `type` で表すため、金額自体は 0 以上であるべき |
| 数量が未設定、または 0 以下 | 同上 |
| 取引日が未設定 | 対象年月の判定ができない |

読み飛ばしが `skip-limit`（既定 1,000）を**超えた**時点で `SkipLimitExceededException` を送出し、
ジョブは異常終了する。件数は再開をまたいで引き継がれる。

## 5. デッドロックの再試行

- 再試行するのはデッドロックだけ。例外の型ではなく、原因例外をたどって
  **MySQL のエラーコード 1213 / SQLState 40001** かどうかで判定する（`DeadlockRetryPolicy`）。
  Spring 7 では例外変換の実装によってデッドロックが `CannotAcquireLockException` などになり、
  型だけでは判別できないため。
- ロック待ちタイムアウト（エラーコード 1205）は別の事象なので再試行しない。
- 再試行回数は既定 3 回、待ち時間は 200ms から 2 倍ずつ（上限 2 秒）。
- 再試行しても失敗した場合はジョブが異常終了する。コミット済みのチャンクはそのまま残るので、
  再実行すれば続きから処理する。

## 6. 運用

### 起動

```bash
java -jar monthly-sales-batch.jar targetMonth=202509
```

cron の例（毎月 1 日 2:00）:

```cron
0 2 1 * * /usr/bin/java -jar /opt/batch/monthly-sales-batch.jar targetMonth=$(date -d '-1 month' +\%Y\%m) >> /var/log/batch/monthly-sales.log 2>&1
```

### 終了コード

| コード | 意味 | 対応 |
|---|---|---|
| 0 | 正常終了 | — |
| 5 | ジョブが異常終了（FAILED） | ログの `[OPS-ALERT]` を確認し、原因を取り除いて同じ引数で再実行（続きから再開） |
| 1 | 起動に失敗（引数不正、同一対象年月で実行済み、DB 接続不可など） | 引数と接続先を確認。実行済みの場合は [3](#最初からやり直したいとき) |

### 通知

異常終了すると、`LoggingOperationNotifier` が次の ERROR ログを出力する。
`[OPS-ALERT]` を監視基盤で拾って運用チームへ通知する想定。

```
[OPS-ALERT] バッチが異常終了しました
  ジョブ名       : monthlySalesSummaryJob
  対象年月       : 202509
  ジョブ実行 ID  : 1
  検知時刻       : 2025-10-01T02:41:13.183
  失敗の内容     : [step=aggregateMonthlySalesStep ... <- SkipLimitExceededException: 不正データの読み飛ばしが上限を超えました: limit=1000 skipped=1001 <- ...]
  再開方法       : 同じ起動引数 (targetMonth=202509) で再実行すると、中断した箇所から再開します
```

メールやチャットへ直接送る場合は `OperationNotifier` の実装を追加して差し替える
（アプリケーション層はポートにしか依存していないので、他の層の変更は不要）。

### DB の準備

1. 業務表を作る: [`src/main/resources/db/mysql/schema.sql`](../src/main/resources/db/mysql/schema.sql)
2. Spring Batch のメタデータ表を作る:
   `spring-batch-core-6.0.5.jar` 内の `org/springframework/batch/core/schema-mysql.sql`

   ```bash
   unzip -p ~/.gradle/.../spring-batch-core-6.0.5.jar org/springframework/batch/core/schema-mysql.sql | mysql -u root sales
   ```

Spring Boot 4 には Spring Batch のメタデータ表を自動生成する設定（かつての
`spring.batch.jdbc.initialize-schema`）が無いため、事前に流しておく必要がある。
ローカル検証用に `local` プロファイルでは `spring.sql.init` で両方を作成する。

```bash
java -jar monthly-sales-batch.jar targetMonth=202509 --spring.profiles.active=local
```

### 設定値

`application.yml` の `batch.monthly-sales.*`。環境変数やコマンドライン引数で上書きできる。

| キー | 既定値 | 意味 |
|---|---|---|
| `chunk-size` | 500 | 1 トランザクションで登録する集計単位の件数 |
| `page-size` | 5000 | 取引明細を 1 回の SELECT で読む件数 |
| `skip-limit` | 1000 | 不正データの読み飛ばしの上限 |
| `deadlock-retry-count` | 3 | デッドロック時の再試行回数 |
| `retry-initial-interval` / `retry-max-interval` | 200ms / 2s | 再試行の待ち時間 |
| `deadline` | 06:00 | 終了期限。超過したら WARN ログ |

## 7. 性能の見積もりと調整

通常 300 万件 / 最大 800 万件を 2:00 から 6:00 までに処理する。

- **読み込み**: `idx_sales_transaction_group (store_id, product_id, transaction_id, transaction_date, type, amount, quantity)`
  により、並べ替え（filesort）も表本体へのランダムアクセスも発生しない索引走査になる。
  OFFSET ではなくキーセットページングなので、後ろのページほど遅くなることもない。
- **書き込み**: チャンク 1 回につき 1 文の複数行 INSERT。
  集計単位（店舗 × 商品）の数が登録件数になるので、明細件数よりはるかに少ない。
- **メモリ**: 保持するのは 1 ページ分の明細（既定 5,000 件）と集計中の 1 グループの合計値だけ。
  明細件数が増えても使用量は変わらない。

遅い場合に見るところ:

1. `EXPLAIN` で `idx_sales_transaction_group` が使われているか（`Using index` が出るか）を確認する。
2. `page-size` / `chunk-size` を増やす（例: 10000 / 1000）。往復回数が減る。
3. MySQL 接続に `rewriteBatchedStatements=true` が付いているか確認する（既定の URL には付与済み）。
4. それでも足りない場合は、`sales_transaction` を取引日で月次パーティション化するか、
   `店舗 ID` でステップを分割（Spring Batch のパーティショニング）して並列化する。
   現在の実装は単一スレッドで、1 回の実行が 1 つの対象年月を処理する。

## 8. 設計上の判断と前提

- **入力表 `sales_transaction` の定義はこのリポジトリで用意した**（仕様に列定義が無いため）。
  既存表がある場合は、列名・型・索引を実環境に合わせて `schema.sql` とマッパーを調整する。
  バッチが前提にしているのは「`transaction_id` / `store_id` / `product_id` / `transaction_date` /
  `type` が NOT NULL で、`amount` / `quantity` は欠損しうる」という点のみ。
- **出力表に `transaction_count`（集計に含めた明細数）を持たせた**。
  仕様には無いが、読み飛ばしが発生したときに「何件を集計したか」を後から確認できるようにするため。
- **集計結果の登録は上書き（upsert）**。仕様は「登録」だが、再実行・再開を安全にするため冪等にしている。
  SQL は `ON DUPLICATE KEY UPDATE ... VALUES(列)` の形（MySQL 8.0.20 以降は行エイリアス
  `AS new` が推奨だが、H2 の MySQL 互換モードでも動く形を優先した）。
- **削除ステップを前段に置いた**。仕様の「登録」だけでは、前回実行時に存在して今回は存在しない
  集計単位の行が残ってしまうため。再開時には実行されないので、再開の妨げにはならない。
- **スケジューラはアプリに持たせていない**。定時起動はジョブスケジューラ（cron、JP1 など）の責務とし、
  バッチは 1 回の実行に専念する。
- **テストは H2（MySQL 互換モード）で実行する**。CI で Docker を使える環境なら、
  Testcontainers で MySQL に対して実行するように差し替えると、SQL の方言差まで検証できる。

## 9. テスト

| テスト | 確認していること |
|---|---|
| `TargetMonthTest` / `RawSalesTransactionTest` / `MonthlySalesAggregationTest` | 値オブジェクトの検証、売上の加算と返品の減算 |
| `InvalidTransactionSkipPolicyTest` | 読み飛ばしの上限、再開時の件数の引き継ぎ、重複計上の防止 |
| `MonthlySalesGroupLoaderTest` | ページ境界をまたぐ集計、読み飛ばし、再開位置の扱い |
| `DeadlockRetryPolicyTest` | デッドロックだけを再試行対象と判定すること |
| `MyBatisRepositoryIntegrationTest` | SQL（キーセットページング、upsert、削除） |
| `MonthlySalesSummaryJobIntegrationTest` | ジョブ全体。集計結果、対象年月の絞り込み、作り直し、読み飛ばしと上限超過、**失敗箇所からの再開**、デッドロックの再試行、二重起動の防止、引数の検証 |
