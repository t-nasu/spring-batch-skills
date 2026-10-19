# 0008 ArchitectureSpec で層構成を検証する

## 背景

レイヤード + DDD の決まりと、domain で Lombok の `@Data` / `@Setter` を使わないことを、プロジェクトの初めから機械的に検証したい（2026-10-19 の依頼）。

## 決定

- プロジェクトを新たに作るとき、業務のコードより先に ArchUnit のテスト `ArchitectureSpec` を置く。雛形はスキルの `templates/ArchitectureSpec.java`
- 名前は依頼どおり `ArchitectureSpec` とし、JUnit 5（ArchUnit の JUnit 5 対応）で書く
- Lombok の `@Data` / `@Setter` は保持期間が SOURCE でクラスファイルに残らず、ArchUnit では直接見えない。そこで「domain に public な setter が無いこと」と「domain のソースに `@Data` / `@Setter` が無いこと」の2つで検出する。前者は手書きの setter も禁じることになるが、domain を不変に保つ趣旨に合うのでそのままとした
- 依頼の2点に加え、domain がフレームワークに依存しないこと、application 層に `Service` という名を使わないことも検証する（どちらも既存の決まり）

## 確認したこと

試験で作った3本（スキルあり）に当てて全ルールが通り、わざと違反を入れたコピー（domain に `@Data` と `@Component`、`@Setter`、application に `BadService` と infrastructure への参照）で5つのルールすべてが失敗することを確かめた。
