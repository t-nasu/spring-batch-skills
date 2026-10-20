---
name: spring-batch-dev
description: Java/Gradle でバッチ処理（Spring Batch または素の Spring Boot、MySQL + MyBatis）を新規に設計・実装するとき、また既存のバッチを変更するときに使う。バッチ処理仕様書からの設計、Batch か素の Boot かの判断、レイヤード + DDD の層構成、使う版の指定を定める。Use when creating or changing a batch job with Spring Batch or Spring Boot.
---

# バッチ処理の開発

このチームのバッチ開発の決まりである。一般的な Spring Batch の知識はすでに持っている前提で、ここではチーム固有の決まりだけを定める。決まりと一般論が食い違うときは、決まりに従う。

## 新規開発の進め方

次の順に進める。設計の承認を得る前にコードを書いてはならない。

1. **仕様を受け取る。** 利用者からバッチ処理仕様書（書式は [references/batch-spec-template.md](references/batch-spec-template.md)）を受け取る。仕様書が無ければ、この書式を示して埋めてもらう。★ 項目の不足だけを、まとめて一度に質問する。
2. **Batch か素の Boot かを決める。** [references/batch-vs-boot.md](references/batch-vs-boot.md) の条件に照らして決め、根拠（仕様のどの項目によるか）を一行で示す。迷ったら素の Spring Boot とする。
3. **設計図と実装計画を書く。** [references/design-diagram.md](references/design-diagram.md) に従い、Mermaid の流れ図とステップ表を `docs/batch-specs/<ジョブ名>.design.md` に書く。[references/implementation-plan.md](references/implementation-plan.md) に従い、使う社内スキルと引数を含む実装計画を書く。両方を利用者に見せて承認を待つ。
4. **実装する。** プロジェクトを新たに作る場合は、業務のコードより先に [references/architecture-spec.md](references/architecture-spec.md) に従って ArchitectureSpec を置く。後の「社内スキルとの関係」に従って社内スキルを使う。社内スキルで定まらない点の層構成と命名は [references/architecture.md](references/architecture.md)、版は [references/versions.md](references/versions.md) に従う。Spring Batch を使う場合は、実装の前に [references/spring-batch-6-pitfalls.md](references/spring-batch-6-pitfalls.md) を読む。異常時の通知は [references/metrics.md](references/metrics.md) に従う。
5. **確かめる。** ArchitectureSpec を含め `./gradlew build` が通ること、設計図とコードが食い違っていないことを確かめ、食い違いがあれば設計図かコードを直す。実装計画の「結果」を追記する。

## 変更の進め方

仕様書（`docs/batch-specs/<ジョブ名>.md`）と設計図（`docs/batch-specs/<ジョブ名>.design.md`）を正本とする。コードだけを直してはならない。

1. **仕様書を直す。** 利用者が仕様書を直していればそれを読む。会話で変更を頼まれた場合は、先に仕様書へ書き戻し、その差分を利用者に示す。
2. **判断を見直す。** 変更により Batch か素の Boot かの判断が変わらないかを [references/batch-vs-boot.md](references/batch-vs-boot.md) で確かめる。変わる場合は、その旨と根拠をまず利用者に伝える。
3. **設計図を直し、実装計画を作る。** 仕様書の変更点を設計図に反映し、新しい実装計画を作る。設計図の変わった箇所（流れ図・ステップ表・クラス一覧）と実装計画を示して承認を待つ。
4. **コードを直す。** ArchitectureSpec が無ければ最初に置く。承認ののち、新規開発の 4・5 と同じ決まりで直し、設計図とコードが食い違っていないことを確かめ、実装計画の「結果」を追記する。

仕様書や設計図が無い既存のバッチを変更する場合は、まずコードから仕様書と設計図を起こし、利用者に確かめてもらってから 1 に進む。

## 社内スキルとの関係

使えるスキルのうち、名前が `add-` で始まるもの（部品を追加する手順）と `review-` で始まるもの（実装を点検する手順）を社内スキルとして扱う。社内スキルは増減するので、決まった名前を当てにせず、作業のたびに使えるスキルの一覧と説明文から、その作業に合うものを選ぶ。

- 実装計画を書くとき、各手順に合う `add-` スキルを選び、手順の表に名前と引数を書く。合うものが無ければ「（なし）」と書き、本スキルの決まりで作る
- 実装の後、合う `review-` スキルをすべて使って点検する
- 社内スキルの手順と決まりに従う。ただし次の点は、社内スキルと食い違っても本スキルに従う
  - バッチやスケジュールタスクの入口（起動処理）を追加する社内スキルは使わない。入口は本スキルの決まりで作る
  - application 層のクラスは `〇〇UseCase` と名付ける。社内スキルが `Service` と書いていても `UseCase` と読み替える
  - chunk の Reader の作り方は、[references/architecture.md](references/architecture.md) の2つの案から開発者が選んだものに従う

社内スキルが1つも無ければ、本スキルの参照資料だけで進める。

## 必ず守ること（要約）

詳細は各参照資料にある。ここに挙げるのは破りやすいものだけである。

- パッケージは `batch` / `application` / `domain` / `infrastructure` の4つ。`presentation` という名は使わない
- `domain` は Spring・MyBatis・Spring Batch の型を import しない。public な setter を置かず、Lombok の `@Data` / `@Setter` も使わない
- ArchitectureSpec のルールを消したり緩めたりしない
- ユースケースのクラス名は `〇〇UseCase`。`〇〇Service` という名は付けない
- MyBatis の SQL はすべて XML に書く。`@Select` などのアノテーション SQL は使わない
- Mapper はテーブル形の DTO を返す。domain のエンティティを resultMap に直接対応させない
- 異常時の通知は Prometheus のカウンタ（`batch.job.failures`, `batch.item.skips`）を増やして行う。メールなどの通知処理は作らない。カウンタを増やすのは batch 層だけ
- 途中からの再開や DB による二重起動防止が求められたら、DB の JobRepository を設定する。既定のままでは記録がメモリにしか残らず、再開できない
- API に迷ったら記憶で書かず、Gradle のキャッシュにある jar を読んで確かめる
- 版は [references/versions.md](references/versions.md) の表に従う。学習時の記憶にある版を使わない。Spring Boot の BOM が管理する依存には版を書かない
