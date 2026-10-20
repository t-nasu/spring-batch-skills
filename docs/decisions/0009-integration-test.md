# 0009 結合テスト

## 背景

- 単体テストだけでは、Spring Batch の構成や起動引数の受け渡しなど、全体を通して正しくつながっているかが分からない（利用者の要望、2026-10-20）
- 実際に近い環境で確かめたい。DB は実の MySQL、AWS は LocalStack を Docker で動かす
- Docker が要るなど実行の条件が厳しく、GitHub Actions では動かせない見込みが高い

## 決定

- 結合テストを `src/integrationTest/groovy` に置き、Gradle の `jvm-test-suite` で `integrationTest` という別のテストの組にする。`check` に結び付けないので、`./gradlew build` では動かない
- コンテナは Testcontainers で動かす（Spring Boot の BOM が版を管理する）
- `@SpringBootTest` ではなく、起動クラスと同じく `SpringApplication.run` に本番と同じ形の起動引数を渡して動かし、終了コードと DB などに残った結果で検査する。起動引数の受け渡しと、Spring Boot によるジョブの自動起動まで含めて確かめるため
- `SpringApplication.run` が例外を投げたときは終了コード 1 とみなす。本番では main から例外が抜けて JVM が 1 で終わるため（完了済みのジョブを同じ引数で起動したときなどに起きる。2026-10-20 に確認）
- 表は本番の DDL で作る。テスト用の DDL を別に持つと、本番との食い違いに気付けないため

## 確認

2026-10-20、Spring Boot 4.1.1・Testcontainers 2.0.5・`mysql:8.4`・`localstack/localstack:4` で試作し、正常系・起動引数の誤り・二重起動・S3 の読み書きが動くこと、`./gradlew build` では動かないことを確かめた。
