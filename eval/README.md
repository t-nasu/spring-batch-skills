# 比較検証（スキルの有無）

スキルが効くかを、同じ課題をスキルあり・なしで作らせて比べる。差が出なければスキルを作る意味は薄い。

## 課題

| 課題 | 期待する判断 | 見どころ |
|---|---|---|
| [t1-expire-points.md](tasks/t1-expire-points.md) | 素の Spring Boot | 小さく再実行してよい処理で Batch を入れないか |
| [t2-monthly-sales.md](tasks/t2-monthly-sales.md) | Spring Batch | 大量件数・途中再開・スキップ・リトライ。Reader の扱い |
| [t3-order-import.md](tasks/t3-order-import.md) | Spring Batch | 複数ステップと条件分岐、ファイル入力、設計図の分岐 |

## 手順（1 課題 × 1 エージェント × スキルあり / なし）

1. 空のディレクトリを作り、`docs/batch-specs/` に課題の仕様書を写す
2. スキルありの場合のみ、`scripts/install.sh <そのディレクトリ>` を実行する
3. そのディレクトリでエージェントを起動し、インターネット接続を切った状態で、次の依頼文を **両方の場合で同じに** 渡す

   > docs/batch-specs/<課題>.md の仕様で、Java・Gradle・MySQL・MyBatis を使い、レイヤードアーキテクチャ + DDD で新規にバッチを作ってください。設計の承認は得たものとして、最後まで進めてください。

4. 終わったら照合する: `python3 eval/check.py <そのディレクトリ> --expect boot|batch [--build]`
5. 下の「目で確かめる項目」も見て、結果を `eval/results.md` に記す

非対話で回す例:
- Claude Code: `claude -p "<依頼文>" --permission-mode acceptEdits --disallowedTools WebFetch WebSearch`
- Codex: `codex exec "<依頼文>"`（ネットワークを切る設定は各自の環境に合わせる）

## 目で確かめる項目（照合スクリプトでは見られないもの）

- Batch / 素の Boot の判断に、仕様のどの項目による根拠が示されているか
- 設計図の流れ図・ステップ表がコードと一致しているか
- t2: Reader が Mapper を直接使う場合、読み取り専用・Reader 内での DTO → ドメイン変換・抽出条件のコメントの三つを守っているか
- t2: スキップ上限 1,000 件、デッドロックのみ 3 回リトライが設定されているか
- t3: 取り込み失敗・集計失敗の両方で通知ステップへ進むか
