# バッチアプリケーションの層構成（レイヤード + DDD）

## パッケージ構成

```
com.example.<アプリ名>
├── batch             起動口。Job/Step の定義、または ApplicationRunner。起動引数の解釈
├── application       ユースケース。トランザクション境界。ドメインの組み立てと呼び出し
├── domain            エンティティ、値オブジェクト、ドメインサービス、リポジトリのインタフェース
└── infrastructure    リポジトリの実装（MyBatis の Mapper と XML）、ファイル入出力、外部連携
```

業務のまとまり（集約）が複数ある場合は、各層の下を集約名で分ける（例: `domain.order`, `infrastructure.order`）。

## 依存の向き

```
batch → application → domain ← infrastructure
```

- domain は他の層に依存しない。Spring・MyBatis・Spring Batch の型を import しない
- infrastructure は domain のリポジトリインタフェースを実装する（依存性の逆転）
- batch から infrastructure を直接呼ばない
- Spring Batch の型（`Job`, `Step`, `ItemReader` など）が現れてよいのは batch と infrastructure のみ

## MyBatis の扱い

- Mapper インタフェースと XML は infrastructure に置く。SQL はすべて XML に書き、アノテーション SQL（`@Select` など）は使わない
- Mapper が返すのはテーブル形の DTO とし、domain のエンティティへの変換はリポジトリ実装の中で行う
- domain のエンティティを MyBatis の resultMap に直接対応させない

## Spring Batch を使う場合の置き場所

| 要素 | 層 | 備考 |
|---|---|---|
| Job / Step の定義（`@Configuration`） | batch | 処理の流れのみを書き、業務判断は書かない |
| ItemProcessor | batch | 中身は application の UseCase を呼ぶだけにする |
| ItemReader / ItemWriter | infrastructure | 下記「chunk の Reader とリポジトリ」参照 |
| 業務ルール | domain | Batch を使わない場合と同じコードになるようにする |

## 命名

- ユースケース: `〇〇UseCase`。`〇〇Service` という名は使わない
- ドメインサービス: `domain` に置き、役割を表す名を付ける（`〇〇Service` としない）
- リポジトリ: domain に `〇〇Repository`、infrastructure に `MyBatis〇〇Repository`
- Mapper: `〇〇Mapper`

## chunk の Reader とリポジトリ

大量件数を読む chunk の Reader の作り方は、ジョブごとに開発者が選ぶ。設計図の段で次の二案を示し、承認のときに選んでもらう。

| 案 | 作り方 | 向く場合 |
|---|---|---|
| A. Mapper を直接使う | `MyBatisCursorItemReader` などで Mapper から読む | 件数が多く、速さとメモリの一定さが要る |
| B. リポジトリを経由する | domain のリポジトリにページ単位で読むメソッドを設け、それを呼ぶ Reader を作る | 層の決まりを崩したくない |

A を選んだ場合は次を守る。

- 読み取り専用に限る。更新は必ずリポジトリを経由する
- Reader は infrastructure に置き、Mapper が返す DTO からドメインオブジェクトへの変換も Reader の中で行う。Processor には常にドメインオブジェクトが渡る
- 処理対象を絞る条件は SQL に書き、その意味を Reader のクラスコメントに業務の言葉で書く
