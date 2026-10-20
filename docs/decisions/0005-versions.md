# 0005 版の指定

## 背景

- Spring Boot / Batch は、スキルでアプリを作る時点での最新版を使いたい
- 利用者のエージェントは Web 検索や Web ページの取得ができない。ただしローカルのコマンド（Gradle、curl など）はインターネットに届く（2026-10-20 に確認）
- 社内の Maven / Gradle の取得先はあるが、版の確認には使わない前提とする

## 決定

（2026-10-20 改訂）スキルに版の表は置かない。エージェントは `templates/check-versions.sh` で、その時点の最新正式版と、それに合う MyBatis Starter・Spock の版を割り出す。割り出せなければ開発者に質問する。表を置くと、Spring Boot を最新にしたときに表の MyBatis Starter や Spock が古くなり、保守の手間もかかるため（利用者の指摘）。

改訂前: スキル内の `references/versions.md` に推奨版の表を置く。エージェントは、決められた curl のコマンドで Maven Central の `maven-metadata.xml` を読んで最新正式版を確かめ、互換を確かめたうえで使う。コマンドが失敗したら表の版を使う。学習時の記憶にある版は使わない。

BOM が管理する依存は BOM に従う。例えば 2026-10-19 時点で Maven Central 上の Connector/J の最新は 26.7.0 だが、Spring Boot 4.1.1 の BOM は 9.7.0 を指定しており、BOM に従う。

`eval/check.py` の版の定数は評価用なので、評価を回す前に `check-versions.sh` の結果に合わせて更新する。
