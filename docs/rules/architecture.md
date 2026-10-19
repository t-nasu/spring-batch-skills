# バッチアプリケーションの層構成（レイヤード + DDD）

> 状態: 草案。「chunk の Reader とリポジトリ」をチームで決めてから確定する。

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
- domain のエンティティを MyBatis の resultMap に直接対応させない（テーブル構造の変更がドメインに波及するのを防ぐ）

## Spring Batch を使う場合の置き場所

| 要素 | 層 | 備考 |
|---|---|---|
| Job / Step の定義（`@Configuration`） | batch | 処理の流れのみを書き、業務判断は書かない |
| ItemProcessor | batch | 中身は application の UseCase を呼ぶだけにする |
| ItemReader / ItemWriter | infrastructure | 下記「chunk の Reader とリポジトリ」参照 |
| 業務ルール | domain | Batch を使わない場合と同じコードになるようにする |

## 命名

- ユースケース: `〇〇UseCase`。`〇〇Service` という名は使わない（何でも「サービス」に入れる習慣を避けるため。ドメインサービスは `domain` に置き、役割を表す名を付ける）
- リポジトリ: domain に `〇〇Repository`、infrastructure に `MyBatis〇〇Repository`
- Mapper: `〇〇Mapper`

## chunk の Reader とリポジトリ（未決）

大量件数の chunk 処理で、Reader がリポジトリを経由せず Mapper を直接使うことを許すか。

### 許す場合

利点
- カーソル読み（`MyBatisCursorItemReader`）で、件数が増えてもメモリ使用量が一定で、速い
- MyBatis 公式の Reader が持つ再起動時の読み取り位置の保存を、そのまま使える
- 書くコードが少ない

欠点
- 読み取りの経路がリポジトリと Reader の二つになる
- 「どのデータを処理対象とするか」という業務条件が SQL に埋もれ、domain から見えなくなる
- 一つ例外を作ると「ここも例外に」と広がりやすい。歯止めのルールが要る

### 許さない場合

利点
- 層のルールに例外がない。読み取りが常に domain のリポジトリ経由で一貫する
- テストでリポジトリを差し替えやすい

欠点
- domain のリポジトリにページ単位で読むメソッドが要り、domain に「ページ」という技術的な概念が入る
- 再起動時の読み取り位置を自前で保存する Reader を書く必要がある（公式 Reader の機能が使えない）
- OFFSET によるページングは件数が多いと遅く、処理中にデータが変わると取りこぼしや重複が起こりうる（キー値で区切るページングで緩和できるが、その分の実装が要る）

### 草案の推し

**条件付きで許す。** 次の歯止めを付ける。
- 読み取り専用の大量処理に限る（更新は必ずリポジトリ経由）
- Reader は infrastructure に置き、Mapper が返す DTO からドメインオブジェクトへの変換も Reader の中で行う。Processor には常にドメインオブジェクトが渡る
- 処理対象を絞る条件は SQL の中に書くが、その意味を Reader のクラスコメントに業務の言葉で書く

