# バッチアプリケーションの層構成（レイヤード + DDD）

> 状態: 草案。末尾の「未決事項」をチームで決めてから確定する。

## パッケージ構成

```
com.example.<アプリ名>
├── presentation      起動口。Job/Step の定義、または ApplicationRunner。起動引数の解釈
├── application       ユースケース。トランザクション境界。ドメインの組み立てと呼び出し
├── domain            エンティティ、値オブジェクト、ドメインサービス、リポジトリのインタフェース
└── infrastructure    リポジトリの実装（MyBatis の Mapper と XML）、ファイル入出力、外部連携
```

業務のまとまり（集約）が複数ある場合は、各層の下を集約名で分ける（例: `domain.order`, `infrastructure.order`）。

## 依存の向き

```
presentation → application → domain ← infrastructure
```

- domain は他の層に依存しない。Spring・MyBatis・Spring Batch の型を import しない
- infrastructure は domain のリポジトリインタフェースを実装する（依存性の逆転）
- presentation から infrastructure を直接呼ばない
- Spring Batch の型（`Job`, `Step`, `ItemReader` など）が現れてよいのは presentation と infrastructure のみ

## MyBatis の扱い

- Mapper インタフェースと XML は infrastructure に置く。SQL は XML に書き、アノテーション SQL は使わない（草案。未決事項参照）
- Mapper が返すのはテーブル形の DTO とし、domain のエンティティへの変換はリポジトリ実装の中で行う
- domain のエンティティを MyBatis の resultMap に直接対応させない（テーブル構造の変更がドメインに波及するのを防ぐ）

## Spring Batch を使う場合の置き場所

| 要素 | 層 | 備考 |
|---|---|---|
| Job / Step の定義（`@Configuration`） | presentation | 処理の流れのみを書き、業務判断は書かない |
| ItemProcessor | presentation | 中身は application のサービスを呼ぶだけにする |
| ItemReader / ItemWriter | infrastructure | 未決事項 1 参照 |
| 業務ルール | domain | Batch を使わない場合と同じコードになるようにする |

## 命名（草案）

- ユースケース: `〇〇UseCase` または `〇〇Service`（未決事項 3）
- リポジトリ: domain に `〇〇Repository`、infrastructure に `MyBatis〇〇Repository`
- Mapper: `〇〇Mapper`

## 未決事項（チームで決める）

1. **chunk の Reader/Writer はリポジトリを経由するか。** 大量件数ではカーソル読み（`MyBatisCursorItemReader` など）が効率的だが、リポジトリを迂回することになる。案 A: 読み取り専用の大量処理に限り infrastructure の Reader が直接 Mapper を使うことを許す。案 B: 必ずリポジトリを経由し、ページング読みにする。草案では **案 A** を推す。
2. **アノテーション SQL を一切禁止するか。** 草案では禁止（XML に統一）。
3. **ユースケースの命名は `UseCase` か `Service` か。**
