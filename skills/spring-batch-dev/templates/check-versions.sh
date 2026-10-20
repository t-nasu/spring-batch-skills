#!/usr/bin/env bash
# 新しいプロジェクトで使う版を、公開されている情報から割り出して表示する。
# どれか1つでも割り出せなければ、終了コード 1 で終わる。
set -uo pipefail

M=https://repo.maven.apache.org/maven2
fetch() { curl -fsS --max-time 20 --retry 5 --retry-delay 10 "$1"; }

# 正式版（数字と点だけの版）のうち最新のもの
latest() {
  fetch "$M/$1/maven-metadata.xml" | grep -oE '<version>[^<]+</version>' | sed 's/<[^>]*>//g' \
    | grep -E "^${2:-[0-9]+\.[0-9]+\.[0-9]+}$" | sort -V | tail -1
}
prop() { grep -oE "<$1>[^<]+</$1>" | head -1 | sed 's/<[^>]*>//g'; }

failed=0
show() { if [ -n "$2" ]; then echo "$1=$2"; else echo "$1=（割り出せなかった）"; failed=1; fi; }

boot=$(latest org/springframework/boot/spring-boot-dependencies)
show spring-boot "$boot"

if [ -n "$boot" ]; then
  bom=$(fetch "$M/org/springframework/boot/spring-boot-dependencies/$boot/spring-boot-dependencies-$boot.pom")
  show spring-batch "$(echo "$bom" | prop spring-batch.version)"
  groovy=$(echo "$bom" | prop groovy.version)
  show groovy "$groovy"

  # MyBatis Starter: Spring Boot と同じメジャー・マイナー版に対応するもののうち最新
  bootMinor=$(echo "$boot" | cut -d. -f1,2)
  mybatis=""
  for v in $(fetch "$M/org/mybatis/spring/boot/mybatis-spring-boot-starter/maven-metadata.xml" \
               | grep -oE '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' | sed 's/<[^>]*>//g' | sort -rV); do
    # 取得に失敗したら、古い版を誤って選ばないよう打ち切る
    pom=$(fetch "$M/org/mybatis/spring/boot/mybatis-spring-boot/$v/mybatis-spring-boot-$v.pom") || break
    sbMinor=$(echo "$pom" | prop spring-boot.version | cut -d. -f1,2)
    if [ "$sbMinor" = "$bootMinor" ]; then mybatis=$v; break; fi
    # 新しい順に見ているので、対応する Spring Boot がより古くなったら打ち切る
    [ "$(printf '%s\n' "$sbMinor" "$bootMinor" | sort -V | head -1)" = "$sbMinor" ] && break
  done
  show mybatis-spring-boot-starter "$mybatis"

  # Spock: Spring Boot が使う Groovy のメジャー・マイナー版に合うもののうち最新
  groovyMinor=$(echo "$groovy" | cut -d. -f1,2)
  show spock-core "$(latest org/spockframework/spock-core "[0-9]+\.[0-9]+-groovy-${groovyMinor//./\\.}")"
fi

show archunit "$(latest com/tngtech/archunit/archunit)"
show aws-sdk-bom "$(latest software/amazon/awssdk/bom)"
show gradle "$(fetch https://services.gradle.org/versions/current | grep -oE '"version" *: *"[^"]+"' | sed 's/.*"\([^"]*\)"$/\1/')"
show foojay-resolver "$(fetch https://plugins.gradle.org/m2/org/gradle/toolchains/foojay-resolver-convention/org.gradle.toolchains.foojay-resolver-convention.gradle.plugin/maven-metadata.xml | prop release)"
show java-lts "$(fetch https://api.adoptium.net/v3/info/available_releases | grep -oE '"most_recent_lts" *: *[0-9]+' | grep -oE '[0-9]+$')"

exit $failed
