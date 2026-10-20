# Spring Batch 6 / Spring Boot 4 の落とし穴

Spring Batch 5 以前の知識と食い違い、しかもコンパイルエラーにならないものを挙げる。Spring Batch を使う場合は実装の前に読む。

ここに無い API で迷ったら、記憶で書かず、依存している jar を読んで確かめる。jar の置き場所は環境で異なるので、パスを決め打ちしない。

1. このスキルの `templates/print-classpath-jars.gradle` を使って、jar の場所を表示する: `./gradlew -q --init-script <スキルのパス>/templates/print-classpath-jars.gradle printClasspathJars`
2. 表示された jar の中身を JDK のツールで調べる: クラスの一覧は `jar tf <jar>`、メソッドの形は `javap -cp <jar> <完全修飾クラス名>`

## 途中からの再開には DB の JobRepository が要る

- 何も設定しないと、JobRepository は実行の記録をメモリにしか残さない（`ResourcelessJobRepository`）。この場合、失敗後に再実行しても失敗した所から再開せず、エラーも出ないまま最初から処理し直す
- 仕様書で「失敗した所から再開したい」、または DB による二重起動防止が求められている場合は、必ず DB の JobRepository を使う。`org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration` を継承した `@Configuration` クラスを batch 層に置く（`@EnableBatchProcessing` と `@EnableJdbcJobRepository` を付ける方法でもよい）。これを置いても、起動時にジョブを走らせる仕組みと終了コードの返却は Spring Boot の自動設定のまま働く
- 記録用の表は自動では作られない（Spring Boot 4 には `spring.batch.jdbc.*` の設定が無い）
  - 本番: spring-batch-core の jar にある `org/springframework/batch/core/schema-mysql.sql` を DB に適用する
  - テスト: `spring.sql.init.schema-locations` に `classpath:org/springframework/batch/core/schema-h2.sql` などを加える
- 6.0 では記録用の表の構成が変わっている（例: `BATCH_STEP_EXECUTION_CONTEXT` は無い）。テストで表名を直に問い合わせず、`JobRepositoryTestUtils.removeJobExecutions()` で後始末する
- 再開は、同じ識別パラメータで同じジョブを起動し直すことで行われる。再開させるジョブに `JobParametersIncrementer` を付けない

## パッケージと名前の変更

- `ItemReader` / `ItemWriter` / `ItemProcessor` / `ItemStreamReader` / `ExecutionContext` / `Chunk` は `org.springframework.batch.infrastructure.item` に、`RepeatStatus` は `org.springframework.batch.infrastructure.repeat` に移った
- `Job` は `core.job`、`Step` / `StepContribution` / `StepExecution` は `core.step`、`JobExecution` は `core.job`、`JobParameters` は `core.job.parameters`、`Tasklet` は `core.step.tasklet`、`SkipListener` は `core.listener` に移った。`ExitStatus` と `BatchStatus` は `core` のまま
- `JobParametersInvalidException` は `core.job.parameters.InvalidJobParametersException` になった
- Spring Boot 側の自動設定は `org.springframework.boot.batch.autoconfigure` にある

## chunk の書き方

- `StepBuilder.chunk(int, transactionManager)` は削除予定の非推奨。`.chunk(件数)` の後に `.transactionManager(tm)` を続ける。スキップ・リトライは `.faultTolerant()`、`.skip(...)`、`.skipLimit(...)`、`.retry(...)`、`.retryLimit(...)` で書く
- `tasklet(tasklet, transactionManager)` は非推奨ではない
- `skipLimit(N)` は N 件までスキップを許し、N+1 件目でステップが失敗する
- リトライは spring-retry ではなく Spring Framework の `org.springframework.core.retry` を使う。何も設定しなければリトライしない

## 例外と終了コード

- MySQL のデッドロックは `CannotAcquireLockException` に変換される。`DeadlockLoserDataAccessException` は非推奨で、実際には投げられないので、リトライの対象にしない
- ジョブの Bean が1つだけあると、起動時に必ず走る（`spring.batch.job.name` を空にしても止まらない）。`@SpringBootTest` では `spring.batch.job.enabled=false` で止める
- 終了コードは `BatchStatus` の順番の値になる。FAILED は 1 ではなく 5。終了コードを返す仕組みは自動で登録されるので、`System.exit(SpringApplication.exit(SpringApplication.run(...)))` だけでよい
