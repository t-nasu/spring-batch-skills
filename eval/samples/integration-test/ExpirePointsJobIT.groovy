package com.example.expirepoints

import groovy.sql.Sql
import java.time.LocalDate
import org.springframework.boot.SpringApplication
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.MountableFile
import spock.lang.Shared
import spock.lang.Specification

/**
 * expirePointsJob の結合テスト。
 *
 * <p>実の MySQL のコンテナを立て、本番と同じ DDL を入れ、本番と同じ起動引数でアプリを動かして、
 * 終了コードと DB に残った結果を確かめる。アプリの Bean は取り出さない。
 */
class ExpirePointsJobIT extends Specification {

    /** 本番と同じ MySQL の版。 */
    static final String MYSQL_IMAGE = 'mysql:8.4'

    static final String TARGET_DATE_ARG = 'targetDate=20261020'
    static final LocalDate TARGET_DATE = LocalDate.of(2026, 10, 20)

    @Shared
    MySQLContainer mysql

    @Shared
    Sql sql

    def setupSpec() {
        mysql = new MySQLContainer(MYSQL_IMAGE)
                .withCopyFileToContainer(MountableFile.forClasspathResource('db/schema-mysql.sql'),
                        '/docker-entrypoint-initdb.d/1-app.sql')
        mysql.start()
        sql = Sql.newInstance(mysql.jdbcUrl, mysql.username, mysql.password)
    }

    def cleanupSpec() {
        sql?.close()
        mysql?.stop()
    }

    def setup() {
        sql.execute('DELETE FROM point_expiration_history')
        sql.execute('DELETE FROM member_point')
    }

    private int runBatch(String... jobArgs) {
        def args = ["--spring.datasource.url=${mysql.jdbcUrl}".toString(),
                    "--spring.datasource.username=${mysql.username}".toString(),
                    "--spring.datasource.password=${mysql.password}".toString()] + jobArgs.toList()
        try {
            SpringApplication.exit(SpringApplication.run(ExpirePointsApplication, args as String[]))
        } catch (Exception ignored) {
            1 // 本番では main から例外が抜け、JVM が終了コード 1 で終わる
        }
    }

    private void insertPoint(long pointId, String memberId, int amount, LocalDate expiresOn, String status) {
        sql.executeInsert('''INSERT INTO member_point (point_id, member_id, amount, expires_on, status)
                             VALUES (?, ?, ?, ?, ?)''', [pointId, memberId, amount, expiresOn, status])
    }

    private List<Map> statuses() {
        sql.rows('SELECT point_id, status FROM member_point ORDER BY point_id')
    }

    private List<Map> histories() {
        // DATE 列は java.sql.Date で返るので、比べやすいよう LocalDate に揃える
        sql.rows('''SELECT point_id, member_id, amount, expired_on
                      FROM point_expiration_history ORDER BY point_id''')
                .collect {
                    [point_id: it.point_id, member_id: it.member_id,
                     amount  : it.amount, expired_on: it.expired_on.toLocalDate()]
                }
    }

    def "有効期限を過ぎた使えるポイントだけを失効させ、履歴を残す"() {
        given:
        insertPoint(1L, 'M0001', 100, LocalDate.of(2026, 10, 19), 'ACTIVE')  // 対象
        insertPoint(2L, 'M0002', 250, LocalDate.of(2026, 1, 31), 'ACTIVE')   // 対象
        insertPoint(3L, 'M0003', 300, LocalDate.of(2026, 10, 20), 'ACTIVE')  // 当日は対象外
        insertPoint(4L, 'M0004', 400, LocalDate.of(2026, 12, 31), 'ACTIVE')  // 期限前
        insertPoint(5L, 'M0005', 500, LocalDate.of(2026, 10, 19), 'EXPIRED') // すでに失効済み

        when:
        int exitCode = runBatch(TARGET_DATE_ARG)

        then:
        exitCode == 0

        and: '対象のポイントだけが EXPIRED になる'
        statuses() == [[point_id: 1L, status: 'EXPIRED'],
                       [point_id: 2L, status: 'EXPIRED'],
                       [point_id: 3L, status: 'ACTIVE'],
                       [point_id: 4L, status: 'ACTIVE'],
                       [point_id: 5L, status: 'EXPIRED']]

        and: '会員 ID・ポイント数・失効日（業務日付）が履歴に残る'
        histories() == [[point_id: 1L, member_id: 'M0001', amount: 100, expired_on: TARGET_DATE],
                        [point_id: 2L, member_id: 'M0002', amount: 250, expired_on: TARGET_DATE]]
    }

    def "対象が 1 件も無くても正常に終わる"() {
        given:
        insertPoint(1L, 'M0001', 100, LocalDate.of(2026, 12, 31), 'ACTIVE')

        when:
        int exitCode = runBatch(TARGET_DATE_ARG)

        then:
        exitCode == 0
        statuses() == [[point_id: 1L, status: 'ACTIVE']]
        histories().empty
    }

    def "成功したあと同じ起動引数で動かし直しても、結果が変わらない（冪等）"() {
        given:
        insertPoint(1L, 'M0001', 100, LocalDate.of(2026, 10, 19), 'ACTIVE')
        assert runBatch(TARGET_DATE_ARG) == 0

        when: '仕様書どおり、最初からやり直す'
        int exitCode = runBatch(TARGET_DATE_ARG)

        then:
        exitCode == 0
        statuses() == [[point_id: 1L, status: 'EXPIRED']]
        histories() == [[point_id: 1L, member_id: 'M0001', amount: 100, expired_on: TARGET_DATE]]
    }

    def "起動引数が正しくなければ、何も変えずに終了コード 1 で終わる: #jobArgs"() {
        given:
        insertPoint(1L, 'M0001', 100, LocalDate.of(2026, 10, 19), 'ACTIVE')

        when:
        int exitCode = runBatch(jobArgs as String[])

        then:
        exitCode == 1
        statuses() == [[point_id: 1L, status: 'ACTIVE']]
        histories().empty

        where:
        jobArgs << [[],                        // 業務日付が無い
                    ['targetDate=2026-10-20'], // 形式が違う
                    ['targetDate=20261332']]   // ありえない日付
    }

    def "不正データがあったら、ジョブを止めて何も変えない"() {
        given: 'ポイント数が負の、業務上ありえない行が混ざっている'
        insertPoint(1L, 'M0001', 100, LocalDate.of(2026, 10, 19), 'ACTIVE')
        insertPoint(2L, 'M0002', -50, LocalDate.of(2026, 10, 19), 'ACTIVE')

        when:
        int exitCode = runBatch(TARGET_DATE_ARG)

        then:
        exitCode == 1

        and: '巻き戻っているので、正しい行も失効していない'
        statuses() == [[point_id: 1L, status: 'ACTIVE'], [point_id: 2L, status: 'ACTIVE']]
        histories().empty
    }

    def "最大想定件数の 20,000 件を失効させられる"() {
        given:
        sql.withBatch(1000, '''INSERT INTO member_point (point_id, member_id, amount, expires_on, status)
                               VALUES (?, ?, ?, ?, ?)''') { stmt ->
            (1..20_000).each { stmt.addBatch([it, "M${it}".toString(), 10, LocalDate.of(2026, 10, 19), 'ACTIVE']) }
        }

        when:
        int exitCode = runBatch(TARGET_DATE_ARG)

        then:
        exitCode == 0
        sql.firstRow("SELECT COUNT(*) AS c FROM member_point WHERE status = 'EXPIRED'").c == 20_000
        sql.firstRow('SELECT COUNT(*) AS c, SUM(amount) AS s FROM point_expiration_history').with {
            c == 20_000 && s == 200_000
        }
    }
}
