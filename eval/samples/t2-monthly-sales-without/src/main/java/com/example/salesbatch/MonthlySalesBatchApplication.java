package com.example.salesbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 月次売上集計バッチの起動クラス。
 *
 * <pre>
 * java -jar monthly-sales-batch.jar targetMonth=202509
 * </pre>
 *
 * <p>Spring Boot の {@code JobLauncherApplicationRunner} が起動引数を
 * ジョブパラメータに変換し、{@code spring.batch.job.name} のジョブを実行する。</p>
 */
@SpringBootApplication
public class MonthlySalesBatchApplication {

    public static void main(String[] args) {
        // バッチなのでジョブ終了後にプロセスを終了させ、終了コードを呼び出し元（ジョブスケジューラ）へ返す
        System.exit(SpringApplication.exit(SpringApplication.run(MonthlySalesBatchApplication.class, args)));
    }
}
