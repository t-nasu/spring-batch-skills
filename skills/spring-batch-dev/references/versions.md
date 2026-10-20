# 使用する版

> 2026-10-19 時点の正式版。

| 対象 | 版 | 指定方法 |
|---|---|---|
| Java | 25 (LTS) | Gradle の toolchain で指定 |
| Gradle | 9.8.1 | Gradle Wrapper |
| Spring Boot | 4.1.1 | `org.springframework.boot` プラグイン |
| Spring Batch | 6.0.5 | Spring Boot の BOM に任せる（版を書かない） |
| MySQL Connector/J | 9.7.0 | Spring Boot の BOM に任せる（版を書かない） |
| MyBatis Spring Boot Starter | 4.1.0 | 明示する（BOM 管理外。Spring Boot 4.1 対応版） |
| Micrometer（`micrometer-registry-prometheus`）・Prometheus Pushgateway 用（`prometheus-metrics-exporter-pushgateway`） | 1.17.1・1.7.0 | Spring Boot の BOM に任せる（版を書かない） |
| ArchUnit（`archunit`） | 1.5.1 | 明示する（BOM 管理外） |
| Spock（`spock-core`） | 2.5-groovy-5.0 | 明示する（BOM 管理外。Groovy 5 用） |
| Groovy | 5.0.8 | Spring Boot の BOM に任せる（版を書かない） |

## エージェントへの指示

1. プロジェクトを新たに作るときは、ローカルのコマンドで Maven Central の最新正式版を確かめる。Web 検索や Web ページの取得は使わず、次のコマンドだけを使う。

   ```bash
   for a in org/springframework/boot/spring-boot \
            org/mybatis/spring/boot/mybatis-spring-boot-starter \
            com/tngtech/archunit/archunit; do
     printf '%s: ' "$a"
     curl -sS --max-time 20 "https://repo1.maven.org/maven2/$a/maven-metadata.xml" \
       | grep -oE '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' | sed 's/<[^>]*>//g' | sort -V | tail -1
   done
   ```

2. 上表より新しい版があれば、次を確かめてから使い、使った版を利用者に伝える
   - MyBatis Starter の POM（`.../mybatis-spring-boot-starter/<版>/mybatis-spring-boot-starter-<版>.pom` の親 POM）の `spring-boot.version` が、使う Spring Boot と同じメジャー・マイナー版であること。合わなければ、合う組み合わせのうち新しいものを使う
   - Spock は上表の版のまま使う（Groovy の版に合わせる必要があるため）
3. コマンドが失敗したとき（届かない・時間切れ）は、上表の版をそのまま使う。学習時の記憶にある版を使ってはならない
4. BOM が管理する依存には版を書かない
