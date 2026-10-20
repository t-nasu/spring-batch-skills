# 使用する版

プロジェクトを新たに作るときは、その時点の最新正式版を使う。学習時の記憶にある版は使わない。

## 手順

1. このスキルの `templates/check-versions.sh` を実行する。Web 検索や Web ページの取得は使わない

   ```bash
   bash <スキルのパス>/templates/check-versions.sh
   ```

   次の版が `名前=版` の形で表示される。

   | 表示名 | 決め方 |
   |---|---|
   | `spring-boot` | 最新正式版 |
   | `spring-batch`、`groovy` | その Spring Boot の BOM が指定する版 |
   | `mybatis-spring-boot-starter` | その Spring Boot と同じメジャー・マイナー版に対応するもののうち最新 |
   | `spock-core` | その Groovy と同じメジャー・マイナー版に合うもののうち最新 |
   | `archunit` | 最新正式版 |
   | `gradle` | 最新正式版 |
   | `foojay-resolver` | 最新正式版（Gradle プラグイン `org.gradle.toolchains.foojay-resolver-convention`） |
   | `java-lts` | 最新の LTS |

2. 1つでも「割り出せなかった」と表示されたら（終了コード 1）、実装に進まず、どの版を使うかを開発者に質問する
3. 使った版を開発者に伝える

## 書き方

- `build.gradle` に版を書くのは、Spring Boot のプラグイン、MyBatis Starter、Spock、ArchUnit だけとする
- Spring Batch、Groovy、MySQL Connector/J など、Spring Boot の BOM が管理する依存には版を書かない
- Java は Gradle の toolchain で、Gradle は Gradle Wrapper で指定する
- 手元にその版の JDK が無くても Gradle が取得できるよう、`settings.gradle` の `plugins` に `id 'org.gradle.toolchains.foojay-resolver-convention' version '<版>'` を書く
