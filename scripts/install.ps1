# スキルを開発対象のプロジェクトへ入れる。Claude Code 用と Codex 用の両方の置き場所へコピーする。
# 使い方（Windows）: powershell -ExecutionPolicy Bypass -File scripts\install.ps1 <開発対象プロジェクトのディレクトリ>
param(
    [Parameter(Mandatory = $true, HelpMessage = '開発対象プロジェクトのディレクトリを指定してください')]
    [string]$Target
)
$ErrorActionPreference = 'Stop'

$src = Join-Path (Split-Path -Parent $PSScriptRoot) 'skills'

foreach ($dir in @((Join-Path $Target '.claude/skills'), (Join-Path $Target '.agents/skills'))) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    foreach ($skill in Get-ChildItem -Directory -Path $src) {
        $dest = Join-Path $dir $skill.Name
        if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
        Copy-Item -Recurse -Path $skill.FullName -Destination $dest
        Write-Output "installed: $dest"
    }
}
