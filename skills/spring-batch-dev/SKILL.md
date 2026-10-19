---
name: spring-batch-dev
description: Java/Gradle でバッチ処理（Spring Batch または素の Spring Boot、MySQL + MyBatis）を新規に設計・実装するときに使う。バッチ処理仕様書からの設計、Batch か素の Boot かの判断、レイヤード + DDD の層構成、使う版の指定を定める。Use when creating a new batch job with Spring Batch or Spring Boot.
---

# バッチ処理の新規開発

このチームのバッチ開発の決まりである。一般的な Spring Batch の知識はすでに持っている前提で、ここではチーム固有の決まりだけを定める。決まりと一般論が食い違うときは、決まりに従う。

## 進め方

次の順に進める。設計の承認を得る前にコードを書いてはならない。

1. **仕様を受け取る。** 利用者からバッチ処理仕様書（書式は [references/batch-spec-template.md](references/batch-spec-template.md)）を受け取る。仕様書が無ければ、この書式を示して埋めてもらう。★ 項目の不足だけを、まとめて一度に質問する。
2. **Batch か素の Boot かを決める。** [references/batch-vs-boot.md](references/batch-vs-boot.md) の条件に照らして決め、根拠（仕様のどの項目によるか）を一行で示す。迷ったら素の Spring Boot とする。
3. **設計図を書く。** [references/design-diagram.md](references/design-diagram.md) に従い、Mermaid の流れ図とステップ表を `docs/batch-specs/<ジョブ名>.design.md` に書く。利用者に見せて承認を待つ。
4. **実装する。** 下の「社内スキルとの関係」に従って社内スキルを使う。社内スキルで定まらない点の層構成と命名は [references/architecture.md](references/architecture.md)、版は [references/versions.md](references/versions.md) に従う。
5. **確かめる。** `./gradlew build` が通ること、設計図とコードが食い違っていないことを確かめ、食い違いがあれば設計図かコードを直す。

## 社内スキルとの関係

次の社内スキルが入っていれば、該当する作業ではそのスキルの手順と決まりに従う。

- ドメイン: `add-aggregate`, `add-domain-model`, `add-value-object`, `add-domain-service`, `add-exception`
- アプリケーション: `add-usecase`
- インフラストラクチャ: `add-repository`, `add-mybatis-mapper`, `add-outboud-kafka-producer`, `add-outbound-s3-client`
- 実装後の点検: `review-layerd-ddd`, `review-dependency`, `review-class-design`, `review-naming`, `review-sql`

ただし次の点は、社内スキルと食い違っても本スキルに従う。

- `add-inbound-batch` は使わない。バッチの入口は本スキルの決まりで作る
- application 層のクラスは `〇〇UseCase` と名付ける。社内スキルが `Service` と書いていても `UseCase` と読み替える
- chunk の Reader の作り方は、[references/architecture.md](references/architecture.md) の二案から開発者が選んだものに従う

社内スキルが入っていなければ、本スキルの参照資料だけで進める。

## 必ず守ること（要約）

詳細は各参照資料にある。ここに挙げるのは破りやすいものだけである。

- パッケージは `batch` / `application` / `domain` / `infrastructure` の四つ。`presentation` という名は使わない
- `domain` は Spring・MyBatis・Spring Batch の型を import しない
- ユースケースのクラス名は `〇〇UseCase`。`〇〇Service` という名は付けない
- MyBatis の SQL はすべて XML に書く。`@Select` などのアノテーション SQL は使わない
- Mapper はテーブル形の DTO を返す。domain のエンティティを resultMap に直接対応させない
- 版は [references/versions.md](references/versions.md) の表に従う。学習時の記憶にある版を使わない。Spring Boot の BOM が管理する依存には版を書かない
