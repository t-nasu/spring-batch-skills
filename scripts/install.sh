#!/usr/bin/env bash
# スキルを開発対象のプロジェクトへ入れる。Claude Code 用と Codex 用の両方の置き場所へ写す。
# 使い方: scripts/install.sh <開発対象プロジェクトのディレクトリ>
set -euo pipefail

target="${1:?開発対象プロジェクトのディレクトリを指定してください}"
src="$(cd "$(dirname "$0")/.." && pwd)/skills"

for dir in "$target/.claude/skills" "$target/.agents/skills"; do
  mkdir -p "$dir"
  for skill in "$src"/*/; do
    name="$(basename "$skill")"
    rm -rf "${dir:?}/$name"
    cp -R "$skill" "$dir/$name"
    echo "installed: $dir/$name"
  done
done
