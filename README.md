# spring-batch-skills

Spring Batch / Spring Boot でバッチを新規開発するための、Claude Code と Codex 向けスキル。

## 中身

- [skills/spring-batch-dev](skills/spring-batch-dev/SKILL.md): スキル本体。チーム固有の決まりは `references/` にある
- [eval](eval/README.md): スキルの有無で出力を比べる課題と照合スクリプト
- [docs/decisions](docs/decisions/README.md): 決まりを定めた経緯と理由（配布しない）
- [scripts/install.sh](scripts/install.sh): 開発対象のプロジェクトへスキルを入れる（`.claude/skills` と `.agents/skills` の両方）

## 計画

1. 判断基準とチームルールを文書化する（済）
2. 最小のスキルにまとめ、スキルの有無で両エージェントの出力を比べる ← いまここ
3. 差が出た部分だけを厚くし、配布形式を整える

## 前提

- チームに配布する。利用者は Claude Code か Codex のどちらかを使う
- 利用時のエージェントはインターネットに接続できない
- Java / Gradle / MySQL / MyBatis、レイヤードアーキテクチャ + DDD
