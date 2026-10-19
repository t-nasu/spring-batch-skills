# 0005 版の指定

## 背景

- Spring Boot / Batch は、スキルでアプリを作る時点での最新版を使いたい
- 利用者のエージェントはインターネットに接続できないため、最新版の確認は努力目標とする
- 社内の Maven / Gradle の取得先はあるが、版の確認には使わない前提とする

## 決定

スキル内の `references/versions.md` に推奨版の表を置く。エージェントは、ネットに接続できれば Maven Central で最新正式版を確かめ、できなければ表の版を使う。学習時の記憶にある版は使わない。

BOM が管理する依存は BOM に従う。例えば 2026-10-19 時点で Maven Central 上の Connector/J の最新は 26.7.0 だが、Spring Boot 4.1.1 の BOM は 9.7.0 を指定しており、BOM に従う。

表の更新手順は [../maintenance/update-versions.md](../maintenance/update-versions.md)。
