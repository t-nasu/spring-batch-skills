# 使用する版

> 確認日: 2026-10-19（Maven Central と services.gradle.org で確認）。正式版のみ（M / RC は除く）。

| 対象 | 版 | 指定方法 |
|---|---|---|
| Java | 25 (LTS) | Gradle の toolchain で指定 |
| Gradle | 9.8.1 | Gradle Wrapper |
| Spring Boot | 4.1.1 | `org.springframework.boot` プラグイン |
| Spring Batch | 6.0.5 | Spring Boot の BOM に任せる（版を書かない） |
| MySQL Connector/J | 9.7.0 | Spring Boot の BOM に任せる（版を書かない） |
| MyBatis Spring Boot Starter | 4.1.0 | 明示する（BOM 管理外。Spring Boot 4.1 対応版） |
| Flyway | 12.4.0 | Spring Boot の BOM に任せる（未決: DB マイグレーションに使うか） |
| Testcontainers | 2.0.5 | Spring Boot の BOM に任せる（未決: テストに使うか） |

注: Maven Central 上の最新は Connector/J 26.7.0 だが、Spring Boot 4.1.1 の BOM は 9.7.0 を指定している。BOM の指定に従う。

## エージェントへの指示

1. インターネットに接続できる場合は、Maven Central の `maven-metadata.xml` で Spring Boot と MyBatis Spring Boot Starter の最新正式版を確かめ、上表より新しければそれを使い、その旨を利用者に伝える。MyBatis Starter は、使う Spring Boot の版に対応していることを POM の `spring-boot.version` で確認する。
2. 接続できない場合は、上表の版をそのまま使う。学習時の記憶にある版を使ってはならない。
3. BOM が管理する依存には版を書かない。

## この表の更新

保守担当がネットに接続できる環境で上表を確かめ、確認日とともに更新する。手順は配布準備の段階で整備する。
