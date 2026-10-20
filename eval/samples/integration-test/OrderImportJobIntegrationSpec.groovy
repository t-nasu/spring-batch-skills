package com.example.orderimport

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import org.springframework.boot.SpringApplication
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.MountableFile
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

/**
 * 本番と同じ起動のしかたで orderImportJob を通しで動かし、
 * 起動引数の受け渡し、CSV の取り込み、集計、再開、二重起動の防止までを確かめる。
 *
 * <p>実の MySQL をコンテナで動かすため Docker が要る。{@code ./gradlew integrationTest} で動かす。
 */
class OrderImportJobIntegrationSpec extends Specification {

    static final String CSV_HEADER = "注文番号,顧客コード,商品コード,数量,単価,注文日時"

    /** 不正データの上限（仕様書 6）。これを超えるとステップが失敗する */
    static final int SKIP_LIMIT = 100

    @Shared
    MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("useSSL", "false")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("org/springframework/batch/core/schema-mysql.sql"),
                    "/docker-entrypoint-initdb.d/1-batch.sql")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("db/schema-mysql.sql"),
                    "/docker-entrypoint-initdb.d/2-app.sql")

    @TempDir
    Path workDir

    def setupSpec() {
        mysql.start()
        execute("INSERT INTO customers (customer_code, customer_name) VALUES ('C-001', '甲商事')")
        execute("INSERT INTO customers (customer_code, customer_name) VALUES ('C-002', '乙物産')")
        execute("INSERT INTO products (product_code, product_name) VALUES ('P-001', '鉛筆')")
        execute("INSERT INTO products (product_code, product_name) VALUES ('P-002', '消しゴム')")
    }

    def cleanupSpec() {
        mysql.stop()
    }

    def "本番と同じ起動引数で、CSV を取り込み日次売上を集計する。不正な行は飛ばす"() {
        given:
        def csv = writeCsv("orders_20261019.csv", [
                "ORD-001,C-001,P-001,2,100.00,2026-10-19 09:00:00",
                "ORD-002,C-002,P-002,3,200.50,2026-10-19 10:00:00",
                "ORD-003,C-999,P-001,1,100.00,2026-10-19 11:00:00",
                "ORD-004,C-001,P-001,0,100.00,2026-10-19 12:00:00",
                "ORD-005,C-001,P-999,1,100.00,2026-10-19 13:00:00",
        ])

        when:
        def exitCode = runBatch("targetDate=20261019", "inputFile=${csv}")

        then: "正常終了する"
        exitCode == 0

        and: "検証を通った注文だけが登録される"
        query("SELECT order_number, quantity, amount FROM orders WHERE business_date = '2026-10-19'"
                + " ORDER BY order_number")*.order_number == ["ORD-001", "ORD-002"]

        and: "業務日付の日次売上が登録される"
        def sales = query("SELECT * FROM daily_sales WHERE sales_date = '2026-10-19'")
        sales.size() == 1
        sales[0].order_count == 2
        sales[0].total_quantity == 5
        sales[0].total_amount == 801.50g
    }

    def "起動引数が足りないと 0 以外の終了コードで終わる: #description"() {
        given:
        def csv = writeCsv("orders_bad_args.csv", ["ORD-101,C-001,P-001,1,100.00,2026-10-20 09:00:00"])

        when:
        def exitCode = runBatch(*args.collect { it.replace("<csv>", csv) })

        then:
        exitCode != 0

        and: "何も登録されない"
        query("SELECT COUNT(*) AS c FROM orders WHERE order_number = 'ORD-101'")[0].c == 0

        where:
        description             | args
        "業務日付が無い"        | ["inputFile=<csv>"]
        "入力ファイルが無い"    | ["targetDate=20261020"]
        "業務日付の形式が違う"  | ["targetDate=2026-10-20", "inputFile=<csv>"]
    }

    def "不正データが上限を超えると失敗し、同じ起動引数で起動し直すと続きから処理して正常終了する"() {
        given: "1500 行のうち 1001〜1101 行目の 101 行が不正（数量 0）な CSV"
        def lines = (1..1500).collect { index ->
            def quantity = (index > 1000 && index <= 1000 + SKIP_LIMIT + 1) ? 0 : 1
            "ORD-2-%04d,C-001,P-001,%d,10.00,2026-10-22 09:00:00".formatted(index, quantity)
        }
        def csv = writeCsv("orders_20261022.csv", lines)

        when: "1 回目を起動する"
        def firstExitCode = runBatch("targetDate=20261022", "inputFile=${csv}")

        then: "不正データが 100 件を超えたため失敗する"
        firstExitCode != 0

        and: "失敗するまでに処理できた分は登録されている"
        def importedAtFirstRun = countOrdersOn("2026-10-22")
        importedAtFirstRun > 0
        importedAtFirstRun < 1500

        and: "集計はまだ行われていない"
        query("SELECT COUNT(*) AS c FROM daily_sales WHERE sales_date = '2026-10-22'")[0].c == 0

        when: "不正データを直して、同じ起動引数で起動し直す"
        writeCsv("orders_20261022.csv", lines.collect { it.replace(",P-001,0,", ",P-001,1,") })
        def secondExitCode = runBatch("targetDate=20261022", "inputFile=${csv}")

        then: "続きから処理して正常終了する"
        secondExitCode == 0

        and: "すべての注文が 1 件ずつ登録されている"
        countOrdersOn("2026-10-22") == 1500

        and: "日次売上が集計されている"
        def sales = query("SELECT * FROM daily_sales WHERE sales_date = '2026-10-22'")
        sales[0].order_count == 1500
        sales[0].total_quantity == 1500
        sales[0].total_amount == 15000.00g
    }

    def "完了したジョブを同じ起動引数で起動すると、0 以外の終了コードで終わり、出力は増えない"() {
        given:
        def csv = writeCsv("orders_20261023.csv", [
                "ORD-301,C-001,P-001,1,100.00,2026-10-23 09:00:00",
                "ORD-302,C-002,P-002,2,100.00,2026-10-23 10:00:00",
        ])
        def firstExitCode = runBatch("targetDate=20261023", "inputFile=${csv}")

        expect:
        firstExitCode == 0
        countOrdersOn("2026-10-23") == 2

        when: "同じ起動引数でもう一度起動する"
        def secondExitCode = runBatch("targetDate=20261023", "inputFile=${csv}")

        then:
        secondExitCode != 0
        countOrdersOn("2026-10-23") == 2
        query("SELECT COUNT(*) AS c FROM daily_sales WHERE sales_date = '2026-10-23'")[0].c == 1
    }

    private int runBatch(String... jobArgs) {
        def args = ["--spring.datasource.url=${mysql.jdbcUrl}".toString(),
                    "--spring.datasource.username=${mysql.username}".toString(),
                    "--spring.datasource.password=${mysql.password}".toString()] + jobArgs.toList()
        try {
            SpringApplication.exit(SpringApplication.run(OrderImportApplication, args as String[]))
        } catch (Exception ignored) {
            1 // 本番では main から例外が抜け、JVM が終了コード 1 で終わる
        }
    }

    private String writeCsv(String fileName, List<String> dataLines) {
        def file = workDir.resolve(fileName)
        Files.writeString(file, ([CSV_HEADER] + dataLines).join(System.lineSeparator()) + System.lineSeparator(),
                StandardCharsets.UTF_8)
        file.toAbsolutePath().toString()
    }

    private int countOrdersOn(String businessDate) {
        query("SELECT COUNT(*) AS c FROM orders WHERE business_date = '${businessDate}'")[0].c as int
    }

    private List<Map<String, Object>> query(String sql) {
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).withCloseable { connection ->
            connection.createStatement().withCloseable { statement ->
                def resultSet = statement.executeQuery(sql)
                def metaData = resultSet.metaData
                def rows = []
                while (resultSet.next()) {
                    def row = [:]
                    (1..metaData.columnCount).each { column ->
                        row[metaData.getColumnLabel(column).toLowerCase()] = resultSet.getObject(column)
                    }
                    rows << row
                }
                rows
            }
        }
    }

    private void execute(String sql) {
        DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).withCloseable { connection ->
            connection.createStatement().withCloseable { statement -> statement.execute(sql) }
        }
    }
}
