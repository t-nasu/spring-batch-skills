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
| ArchUnit（`archunit`） | 1.5.1 | 明示する（BOM 管理外） |
| Spock（`spock-core`） | 2.5-groovy-5.0 | 明示する（BOM 管理外。Groovy 5 用） |
| Groovy | 5.0.8 | Spring Boot の BOM に任せる（版を書かない） |

## エージェントへの指示

1. インターネットに接続できる場合は、Maven Central の `maven-metadata.xml` で Spring Boot と MyBatis Spring Boot Starter の最新正式版を確かめ、上表より新しければそれを使い、その旨を利用者に伝える。MyBatis Starter は、使う Spring Boot の版に対応していることを POM の `spring-boot.version` で確認する。
2. 接続できない場合は、上表の版をそのまま使う。学習時の記憶にある版を使ってはならない。
3. BOM が管理する依存には版を書かない。

