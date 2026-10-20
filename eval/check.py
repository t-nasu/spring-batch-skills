#!/usr/bin/env python3
"""生成されたバッチプロジェクトがチームの決まりを守っているかを機械的に確かめる。

使い方: python3 eval/check.py <プロジェクトのディレクトリ> --expect batch|boot [--build]
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path

LAYERS = {"batch", "application", "domain", "infrastructure"}
BOOT_VERSION = "4.1.1"
MYBATIS_STARTER_VERSION = "4.1.0"
JAVA_VERSION = "25"
ARCHUNIT_VERSION = "1.5.1"
SPOCK_VERSION = "2.5-groovy-5.0"
GRADLE_VERSION = "9.8.1"
BOM_MANAGED = ["org.springframework.batch:spring-batch-core", "com.mysql:mysql-connector-j",
               "org.springframework.boot:spring-boot-starter"]


def read_all(paths):
    return {p: p.read_text(encoding="utf-8", errors="replace") for p in paths}


def find_layer_dirs(java_root):
    """層のディレクトリが並ぶ親（アプリのルートパッケージ）を探す。"""
    for d in sorted(java_root.rglob("*")):
        if d.is_dir() and {c.name for c in d.iterdir() if c.is_dir()} & (LAYERS | {"presentation"}):
            return d
    return None


def check(project, expect, build):
    results = []

    def add(name, ok, detail=""):
        results.append((name, ok, detail))

    designs = list(project.glob("docs/batch-specs/*.design.md"))
    add("設計図がある（docs/batch-specs/*.design.md）", bool(designs))
    add("設計図に Mermaid の図がある",
        any("```mermaid" in t for t in read_all(designs).values()))

    java_root = project / "src/main/java"
    java_files = read_all(java_root.rglob("*.java")) if java_root.exists() else {}
    app_root = find_layer_dirs(java_root) if java_root.exists() else None
    layers = {c.name for c in app_root.iterdir() if c.is_dir()} if app_root else set()
    add("4層のパッケージがそろっている", LAYERS <= layers, f"見つかった層: {sorted(layers)}")
    add("presentation パッケージが無い", "presentation" not in layers)

    bad = [p.name for p, t in java_files.items()
           if "/domain/" in p.as_posix()
           and re.search(r"^import\s+(org\.springframework|org\.apache\.ibatis|org\.mybatis)", t, re.M)]
    add("domain が Spring/MyBatis を import しない", not bad, ", ".join(bad))

    services = [p.name for p in java_files if p.stem.endswith("Service") and "/domain/" not in p.as_posix()]
    usecases = [p.name for p in java_files if p.stem.endswith("UseCase")]
    add("UseCase という名のクラスがある", bool(usecases))
    add("Service という名のクラスが無い（domain 以外）", not services, ", ".join(services))

    add("ArchitectureSpec がある", any(project.glob("src/test/groovy/**/ArchitectureSpec.groovy")))

    anno = [p.name for p, t in java_files.items() if re.search(r"@(Select|Insert|Update|Delete)\s*\(", t)]
    xmls = [p for p in project.glob("src/main/resources/**/*.xml") if "<mapper" in p.read_text(errors="replace")]
    add("アノテーション SQL を使っていない", not anno, ", ".join(anno))
    add("Mapper XML がある", bool(xmls))

    gradle_files = read_all([p for p in (project / "build.gradle", project / "build.gradle.kts") if p.exists()])
    g = "\n".join(gradle_files.values())
    props_file = project / "gradle.properties"
    props = dict(re.findall(r"^\s*([\w.]+)\s*=\s*(.+?)\s*$", props_file.read_text(), re.M)) if props_file.exists() else {}
    # gradle.properties の値で ${name}・$name・JavaLanguageVersion.of(name as Integer) を置き換える
    for k, v in props.items():
        g = g.replace("${" + k + "}", v).replace("$" + k, v)
    g = re.sub(r"JavaLanguageVersion\.of\(\s*(\w+)(?:\s+as\s+Integer)?\s*\)",
               lambda m: f"JavaLanguageVersion.of({props.get(m.group(1), m.group(1))})", g)
    add(f"Spring Boot {BOOT_VERSION}", bool(re.search(rf"org\.springframework\.boot['\"]?\)?\s*version\s*\(?['\"]{re.escape(BOOT_VERSION)}", g)))
    add(f"MyBatis Starter {MYBATIS_STARTER_VERSION}", f"mybatis-spring-boot-starter:{MYBATIS_STARTER_VERSION}" in g)
    add(f"Spock {SPOCK_VERSION}", f"spock-core:{SPOCK_VERSION}" in g)
    add(f"ArchUnit {ARCHUNIT_VERSION}", f"com.tngtech.archunit:archunit:{ARCHUNIT_VERSION}" in g)
    add(f"Java {JAVA_VERSION} の toolchain",
        bool(re.search(rf"languageVersion\s*(=|\.set\()\s*JavaLanguageVersion\.of\(\s*{JAVA_VERSION}\s*\)", g)))
    pinned = [m for m in BOM_MANAGED if re.search(rf"{m}[\w-]*:\d", g)]
    add("BOM 管理の依存に版を書いていない", not pinned, ", ".join(pinned))

    wrapper = project / "gradle/wrapper/gradle-wrapper.properties"
    add(f"Gradle Wrapper {GRADLE_VERSION}",
        wrapper.exists() and f"gradle-{GRADLE_VERSION}-" in wrapper.read_text())

    uses_batch = "spring-boot-starter-batch" in g
    add(f"判断が期待どおり（{expect}）", uses_batch == (expect == "batch"),
        "Spring Batch を使用" if uses_batch else "素の Spring Boot")
    specs = "\n".join(p.read_text(encoding="utf-8", errors="replace")
                      for p in project.glob("docs/batch-specs/*.md") if not p.name.endswith(".design.md"))
    if uses_batch and re.search(r"失敗した所から再開したい|二重起動を防ぐ必要があるか: ある", specs):
        all_java = "\n".join(java_files.values())
        add("再開のため DB の JobRepository を設定している",
            bool(re.search(r"JdbcDefaultBatchConfiguration|@EnableJdbcJobRepository|JdbcJobRepositoryFactoryBean", all_java)))
    it_files = read_all(project.glob("src/integrationTest/**/*.groovy"))
    add("結合テストが src/integrationTest にある", bool(it_files))
    add("結合テストの組（integrationTest）がある", bool(re.search(r"integrationTest\s*\(\s*JvmTestSuite", g)))
    it_text = "\n".join(it_files.values())
    add("結合テストで実の MySQL コンテナを使う", "MySQLContainer" in it_text)
    add("結合テストを本番と同じ起動のしかたで動かす", "SpringApplication.run" in it_text and "@SpringBootTest" not in it_text)
    if expect == "boot":
        all_java = "\n".join(java_files.values())
        add("ApplicationRunner を使う", "ApplicationRunner" in all_java)
        add("ExitCodeGenerator で終了コードを返す", "ExitCodeGenerator" in all_java)

    if build:
        gradlew = project / "gradlew"
        cmd = [str(gradlew) if gradlew.exists() else "gradle", "build", "-q"]
        r = subprocess.run(cmd, cwd=project, capture_output=True, text=True)
        add("ビルドが通る", r.returncode == 0, r.stderr.strip().splitlines()[-1] if r.returncode else "")

    return results


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("project", type=Path)
    ap.add_argument("--expect", choices=["batch", "boot"], required=True)
    ap.add_argument("--build", action="store_true")
    a = ap.parse_args()
    results = check(a.project, a.expect, a.build)
    for name, ok, detail in results:
        print(f"{'○' if ok else '×'} {name}" + (f"  ({detail})" if detail and not ok else ""))
    passed = sum(ok for _, ok, _ in results)
    print(f"\n{passed}/{len(results)}")
    sys.exit(0 if passed == len(results) else 1)


if __name__ == "__main__":
    main()
