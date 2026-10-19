# ArchitectureSpec（アーキテクチャの検証）

層構成と domain の決まりを、ArchUnit のテストで機械的に検証する。

## 置き方

プロジェクトを新たに作るときは、業務のコードを書く前に置く。既存のプロジェクトに無ければ、変更の最初に置く。

1. [../templates/ArchitectureSpec.java](../templates/ArchitectureSpec.java) を `src/test/java/<ルートパッケージのパス>/ArchitectureSpec.java` にコピーし、`__ROOT_PACKAGE__` をルートパッケージ名（`@SpringBootApplication` を付けたクラスのパッケージ）に置き換える
2. テストの依存に ArchUnit を加える（版は [versions.md](versions.md)。BOM 管理外なので版を書く）

   ```groovy
   testImplementation 'com.tngtech.archunit:archunit-junit5:<版>'
   ```

3. `./gradlew test` で ArchitectureSpec が通ることを確かめる

## 検証すること

| ルール | 内容 |
|---|---|
| `layers` | 依存の向きが `batch → application → domain ← infrastructure` のみであること |
| `domainIsFrameworkFree` | domain が Spring・MyBatis・Spring Batch に依存しないこと |
| `noServiceInApplication` | application 層に `〇〇Service` という名のクラスが無いこと |
| `noPublicSettersInDomain` | domain に public な setter が無いこと（Lombok の `@Data` / `@Setter` が生成するものを含む） |
| `noLombokDataOrSetterInDomain` | domain のソースで Lombok の `@Data` / `@Setter` を使っていないこと |

Lombok の `@Data` / `@Setter` はクラスファイルに残らないため、生成される setter の有無と、ソースの記述の両方で検出する。

## 守ること

- ArchitectureSpec のルールを消したり緩めたりしない。違反したらコードを直す
- ルールを足すのはよい
