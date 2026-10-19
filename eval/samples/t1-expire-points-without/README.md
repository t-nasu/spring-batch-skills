# point-batch — expirePointsJob

`docs/batch-specs/t1-expire-points.md` の仕様にもとづく、ポイント失効バッチです。

有効期限を過ぎた会員ポイントを失効させ、失効履歴を登録します。

## 技術スタック

| 項目 | 採用 |
|---|---|
| 言語 | Java 21 |
| ビルド | Gradle 8.14 (Wrapper 同梱) |
| フレームワーク | Spring Boot 3.3.5 (Web なし / CommandLineRunner 起動) |
| DB | MySQL 8.0 |
| O/R マッパー | MyBatis (mybatis-spring-boot-starter 3.0.3) |
| テスト | JUnit 5 / AssertJ / Mockito / H2 (MySQL 互換モード) |

## アーキテクチャ

レイヤードアーキテクチャ + DDD。依存の向きは `presentation → application → domain ← infrastructure` で、
ドメイン層は他のどの層にも依存しません。

```
com.example.pointbatch
├── presentation                     起動口・起動引数・終了コード
│   ├── ExitCode
│   └── expirepoints
│       ├── ExpirePointsJobRunner        CommandLineRunner / ExitCodeGenerator
│       ├── ExpirePointsJobArguments     targetDate=yyyyMMdd の解析
│       └── JobArgumentException
├── application                      ユースケースの組み立てとトランザクション境界
│   └── expirepoints
│       ├── ExpirePointsUseCase          ユースケースのインターフェース
│       ├── ExpirePointsService          チャンクの繰り返し制御
│       ├── ExpirePointsChunkProcessor   1 チャンク = 1 トランザクション
│       ├── ExpirePointsChunkResult / ExpirePointsResult
│       └── ExpirePointsProperties       chunk-size 設定
├── domain                           業務ルール。フレームワーク非依存。
│   ├── model
│   │   ├── shared/BusinessDate          業務日付 (値オブジェクト)
│   │   ├── member/MemberId
│   │   └── point
│   │       ├── MemberPoint              集約ルート。失効の可否判定と失効操作を持つ
│   │       ├── PointExpirationHistory   失効履歴
│   │       ├── MemberPointId / PointAmount / PointStatus
│   ├── repository                       リポジトリのインターフェース (実装は infrastructure)
│   └── exception                        InvalidPointData / IllegalPointState / PointUpdateConflict
└── infrastructure                   永続化の実装
    └── datasource/mybatis
        ├── MyBatisMemberPointRepository / MyBatisPointExpirationHistoryRepository
        ├── mapper/                      MyBatis Mapper インターフェース
        └── record/                      テーブル行の DTO (ドメインモデルとは分離)
```

業務ルールの置き場所:

- 「有効期限当日はまだ有効、翌日以降が失効対象」の判定 … `MemberPoint#isExpiredAsOf`
- 「有効なポイントだけを失効させ、失効履歴を作る」 … `MemberPoint#expire`
- 「不正な値は受け付けない」 … 各値オブジェクトのコンストラクタ

SQL の抽出条件 `status = 'ACTIVE' AND expires_on < :targetDate` と
`MemberPoint#canExpire` の判定は同じルールを表しています。SQL 側は性能のための絞り込み、
ドメイン側は防御として二重に判定しています。

## 処理フロー

1. `ExpirePointsJobRunner` が起動引数 `targetDate=yyyyMMdd` を解析する
2. `ExpirePointsService` がチャンク単位の処理を、対象が無くなるまで繰り返す
3. `ExpirePointsChunkProcessor` が 1 トランザクションで
   1. 失効対象を ID 昇順で最大 `chunk-size` 件取得
   2. 各 `MemberPoint` を `expire()` して失効履歴を生成
   3. `member_point` を `EXPIRED` に更新
   4. `point_expiration_history` に一括登録
4. 結果件数をログに出し、終了コードを返す

### チャンク分割とカーソル

ID によるキーセットページング (`WHERE ... AND id > :lastId ORDER BY id LIMIT :chunkSize`) を使います。
OFFSET 方式と違い、更新によって抽出結果が変化しても取りこぼしや重複が起きません。

既定の `chunk-size` は 1,000 件です。最大想定の 20,000 件でも 20 回のコミットで終わります。

## 起動方法

```bash
./gradlew bootJar
java -jar build/libs/point-batch.jar targetDate=20260401
```

`--targetDate=20260401` 形式も受け付けます。

### 接続設定 (環境変数)

| 変数 | 既定値 |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `point` |
| `DB_USER` | `point` |
| `DB_PASSWORD` | `point` |

チャンクサイズは `--batch.expire-points.chunk-size=2000` で上書きできます。

### 終了コード

| コード | 意味 | スケジューラの扱い |
|---|---|---|
| `0` | 正常終了 | 正常 |
| `1` | 処理中にエラーが発生し、ジョブを中断した | 異常。監視が検知する |
| `2` | 起動引数が不正 | 異常。設定ミスなので再実行しても直らない |

## DDL

`src/main/resources/db/schema-mysql.sql` を適用してください。

```sql
member_point              (id, member_id, amount, status, expires_on, created_at, updated_at)
point_expiration_history  (id, member_point_id, member_id, amount, expired_on, created_at)
```

抽出用に `member_point (status, expires_on, id)` の複合インデックスを張っています。

## 異常時の考え方 (仕様 6 章の実装)

- **途中で失敗したとき**: チャンクごとにコミットしているため、途中まで失効した状態でジョブが止まります。
  失効済みのレコードは抽出条件 `status = 'ACTIVE'` から外れるので、**同じ引数で最初から再実行すれば未処理分だけが処理されます**。
  念のため `point_expiration_history.member_point_id` に UNIQUE 制約を張り、履歴の二重登録を DB レベルでも防いでいます。
- **不正データがあったとき**: 値オブジェクトのコンストラクタで検出して `InvalidPointDataException` を送出し、
  そのチャンクをロールバックしてジョブを止めます (終了コード 1)。スキップはしません。
- **一時的なエラーの再試行**: 行いません。例外はそのまま終了コードに変換します。
- **通知**: アプリからは通知しません。終了コードとログでスケジューラの監視に委ねます。
- **二重起動防止**: 実装していません (スケジューラが排他する前提)。
  なお `UPDATE ... WHERE status = 'ACTIVE'` の更新件数を検証しているため、万一並行実行されても
  同じポイントを二重に失効させることはなく、競合を検知してジョブが止まります
  (`PointUpdateConflictException`)。

## テスト

```bash
./gradlew test
```

| テスト | 内容 |
|---|---|
| `MemberPointTest` / `ValueObjectTest` | 業務ルールと不正データ判定 (境界値含む) |
| `ExpirePointsServiceTest` | チャンク繰り返しとカーソル前進 (Mockito) |
| `ExpirePointsJobArgumentsTest` | 起動引数の解析 |
| `ExpirePointsJobRunnerTest` | 例外から終了コードへの変換 |
| `MyBatisMemberPointRepositoryTest` | 抽出条件・キーセットページング・更新競合検知 |
| `ExpirePointsIntegrationTest` | ユースケース全体 (対象の選別・チャンクまたぎ・再実行の冪等性) |

> **注意**: 本リポジトリの検証環境には MySQL を用意できないため、DB を使うテストは
> H2 の MySQL 互換モード (`MODE=MySQL`) で実行しています。テスト用スキーマは
> `src/test/resources/db/schema-h2.sql` にあり、本番 DDL とは別ファイルです。
> **両方を同期して更新してください。** 本番相当の確認は MySQL 8.0 を用意して行う必要があります。

## 仕様から補った判断

仕様に明記がなく、実装上の判断で決めた点です。認識が違う場合は指摘してください。

1. **失効履歴の「失効日」= 業務日付 (`targetDate`)**。
   有効期限 (`expires_on`) ではなく、失効処理を行った日を記録しています。
   `member_point_id` も保持しているので、元の有効期限は結合で追跡できます。
2. **`expires_on < targetDate` の境界**。仕様の抽出条件をそのまま採用し、
   有効期限当日はまだ有効、翌日以降を失効対象としています。
3. **`amount = 0` は正常、負値は不正データ**。0 は使い切った残高としてありうるため許容します。
4. **`status` の値域は `ACTIVE` / `USED` / `EXPIRED`**。仕様に出てくるのは `ACTIVE` と `EXPIRED` のみですが、
   ポイント業務で通常必要になる `USED` を加えています。未知の値を読んだ場合は不正データとして停止します。
5. **Spring Batch は使っていません**。リスタートはジョブ全体の再実行でよく、リトライもスキップも不要という
   仕様のため、メタデータテーブルを必要とする Spring Batch は過剰と判断しました。
6. **二重起動防止・通知は実装していません** (仕様でスケジューラに委ねるとされているため)。
