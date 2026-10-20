# ArchitectureSpec（アーキテクチャの検証）

層構成と domain の決まりを、Spock と ArchUnit のテストで機械的に検証する。

## 置き方

プロジェクトを新たに作るときは、業務のコードを書く前に置く。既存のプロジェクトに無ければ、変更の最初に置く。

1. [../templates/ArchitectureSpec.groovy](../templates/ArchitectureSpec.groovy) を `src/test/groovy/<ルートパッケージのパス>/ArchitectureSpec.groovy` にコピーし、`__ROOT_PACKAGE__` をルートパッケージ名（`@SpringBootApplication` を付けたクラスのパッケージ）に置き換える
2. `build.gradle` に `groovy` プラグインとテストの依存を加える（版は [versions.md](versions.md)。Spock と ArchUnit は BOM 管理外なので版を書く。Groovy は BOM に任せる）

   ```groovy
   plugins {
       id 'groovy'
       // 既存のプラグイン
   }

   dependencies {
       testImplementation 'org.spockframework:spock-core:<版>'
       testImplementation 'com.tngtech.archunit:archunit:<版>'
   }
   ```

   テストは JUnit Platform で動かす（`tasks.named('test') { useJUnitPlatform() }`）

3. `./gradlew test` で ArchitectureSpec が通ることを確かめる

## 検証すること

| フィーチャー | 内容 |
|---|---|
| 依存の向き | `batch → application → domain ← infrastructure` のみであること |
| domain のフレームワーク非依存 | domain が Spring・MyBatis・Spring Batch に依存しないこと |
| application 層の命名 | `〇〇Service` という名のクラスが無いこと |
| domain の setter | public な setter が無いこと（Lombok の `@Data` / `@Setter` が生成するものを含む） |
| domain の Lombok | ソースで Lombok の `@Data` / `@Setter` を使っていないこと |

Lombok の `@Data` / `@Setter` はクラスファイルに残らないため、生成される setter の有無と、ソースの記述の両方で検出する。

## 守ること

- ArchitectureSpec のフィーチャーを消したり緩めたりしない。違反したらコードを直す
- フィーチャーを足すのはよい
