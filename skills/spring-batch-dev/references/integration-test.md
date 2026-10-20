# 結合テスト

アプリを本番と同じ起動のしかたで動かし、ジョブの構成、起動引数の受け渡し、DB や AWS とのやり取りまで通しで正しくつながっているかを確かめる。DB は実の MySQL、AWS は LocalStack を Docker のコンテナで動かす（Testcontainers を使う）。

Docker が要るため、単体テストとは分ける。

- 置き場所は `src/integrationTest/groovy`（Spock）。単体テストの `src/test` には置かない
- `./gradlew build` と `./gradlew check` では動かさない。`./gradlew integrationTest` で動かす
- 単体テストは H2 などで今までどおり書く。結合テストは単体テストの代わりにしない

## build.gradle

Gradle の `jvm-test-suite` で、テストの組を1つ足す。`check` に結び付けないので、`build` では動かない。

```groovy
testing {
    suites {
        // 実の MySQL などをコンテナで動かすため Docker が要る。build / check には含めない
        // 実行: ./gradlew integrationTest
        integrationTest(JvmTestSuite) {
            useJUnitJupiter()
            dependencies {
                implementation project()
                implementation 'org.spockframework:spock-core:<版>'
                implementation 'org.springframework.boot:spring-boot-starter-test'
                implementation 'org.testcontainers:testcontainers-mysql'
                // AWS を使う場合だけ
                implementation 'org.testcontainers:testcontainers-localstack'
            }
        }
    }
}
```

- Testcontainers は Spring Boot の BOM が管理するので版を書かない。Testcontainers 2 では、モジュール名が `testcontainers-mysql` のように `testcontainers-` で始まる。クラスは `org.testcontainers.mysql.MySQLContainer` と `org.testcontainers.localstack.LocalStackContainer` を使う（`org.testcontainers.containers` の下にある同じ名前のクラスは古い）
- AWS SDK を使う場合は、`software.amazon.awssdk:bom` を `platform(...)` で取り込み、版は [versions.md](versions.md) の手順で割り出す

## コンテナ

- MySQL のイメージは、本番と同じ版にする（例: `mysql:8.4`）。本番の版が仕様書に無ければ、開発者に質問する
- 表は、MySQL の `/docker-entrypoint-initdb.d/` に SQL ファイルを置いて作る。本番と同じ DDL を使い、テスト用の DDL を別に書かない
  - DB の JobRepository を使う場合: spring-batch-core の jar にある `org/springframework/batch/core/schema-mysql.sql` を先に置く
  - 業務表の DDL: `src/main/resources` にある本番用のもの
- LocalStack は、使うサービスだけを `withServices(...)` で起動する
- コンテナは Spock の `@Shared` に置き、`setupSpec` で起動、`cleanupSpec` で止める。フィーチャーごとに起動し直さない

```groovy
@Shared
MySQLContainer mysql = new MySQLContainer('mysql:8.4')
        .withCopyFileToContainer(MountableFile.forClasspathResource('org/springframework/batch/core/schema-mysql.sql'),
                '/docker-entrypoint-initdb.d/1-batch.sql')
        .withCopyFileToContainer(MountableFile.forClasspathResource('db/schema-mysql.sql'),
                '/docker-entrypoint-initdb.d/2-app.sql')
```

## アプリの起動のしかた

- `@SpringBootTest` は使わない。起動クラスと同じく `SpringApplication.run(<起動クラス>, args)` で起動し、`SpringApplication.exit(...)` で終了コードを受け取る。起動クラスの `main` は `System.exit` を呼ぶので、`main` は呼ばない
- `args` には、本番と同じ形の起動引数（例: `targetDate=20261022`）と、接続先を差し替える `--spring.datasource.url=...` などを並べる。接続先はコンテナから得る（`mysql.jdbcUrl`、`localstack.endpoint` など）
- AWS のクライアントは、接続先（endpoint）を設定値で差し替えられるように作る。結合テストではそこに LocalStack の endpoint を渡す
- 起動中に例外が出ると、`SpringApplication.run` は例外を投げる（例: 完了済みのジョブを同じ起動引数で起動したとき）。本番では JVM が終了コード 1 で終わるので、テストでも例外を受けたら終了コード 1 として扱う

```groovy
private int runBatch(String... jobArgs) {
    def args = ["--spring.datasource.url=${mysql.jdbcUrl}",
                "--spring.datasource.username=${mysql.username}",
                "--spring.datasource.password=${mysql.password}"] + jobArgs.toList()
    try {
        SpringApplication.exit(SpringApplication.run(OrderImportApplication, args as String[]))
    } catch (Exception e) {
        1 // 本番では main から例外が抜け、JVM が終了コード 1 で終わる
    }
}
```

- 検査は、終了コードと、DB・S3 などに残った結果で行う。アプリの Bean を取り出して検査しない

## 書くケース

仕様書から、少なくとも次を書く。

- 正常系: 本番と同じ起動引数で起動し、終了コードが 0 で、出力（DB の表、S3 のファイルなど）が期待どおりであること
- 起動引数の誤り: 必須の起動引数が無い、形式が違うときに、終了コードが 0 以外になること
- 途中からの再開が求められている場合: 1回目を失敗させ、同じ起動引数で起動し直すと、続きから処理して正常終了すること
- 二重起動の防止が求められている場合: 完了したジョブを同じ起動引数で起動すると、終了コードが 0 以外になり、出力が増えないこと
