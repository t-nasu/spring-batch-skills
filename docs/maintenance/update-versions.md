# 版の表の更新手順

保守担当が、インターネットに接続できる環境で行う。

1. Maven Central で最新正式版を確かめる（M / RC は除く）

   ```bash
   for a in org/springframework/boot/spring-boot org/mybatis/spring/boot/mybatis-spring-boot-starter com/tngtech/archunit/archunit; do
     curl -sS https://repo1.maven.org/maven2/$a/maven-metadata.xml \
       | grep -oE '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' | sed 's/<[^>]*>//g' | sort -V | tail -1
   done
   curl -sS https://services.gradle.org/versions/current | grep '"version"'
   ```

2. Spock は `org/spockframework/spock-core/maven-metadata.xml` から、BOM の Groovy のメジャー版に合う `-groovy-<版>` の最新正式版を選ぶ
3. MyBatis Starter の POM の `spring-boot.version` が、使う Spring Boot の版に対応していることを確かめる
4. Spring Boot の BOM（`spring-boot-dependencies-<版>.pom`）で、Spring Batch と Connector/J の版を読む
5. `skills/spring-batch-dev/references/versions.md` の表と日付を更新する
6. `eval/check.py` の版の定数も合わせて更新する
