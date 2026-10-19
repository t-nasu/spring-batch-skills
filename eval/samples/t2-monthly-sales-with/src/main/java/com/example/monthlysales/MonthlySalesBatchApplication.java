package com.example.monthlysales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 月次売上集計バッチの起動口。
 *
 * <p>ジョブの成否をスケジューラが判定できるよう、終了コードを返して終わる。
 * 起動例: {@code java -jar monthly-sales-batch.jar targetMonth=202509}
 */
@SpringBootApplication
public class MonthlySalesBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(MonthlySalesBatchApplication.class, args)));
    }
}
