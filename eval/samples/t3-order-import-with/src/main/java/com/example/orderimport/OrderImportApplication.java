package com.example.orderimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * orderImportJob の起動口。
 *
 * <p>スケジューラが成否を判定できるよう、ジョブの結果を終了コードで返す
 * （Spring Boot の {@code JobExecutionExitCodeGenerator} が自動で登録される）。
 */
@SpringBootApplication
public class OrderImportApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(OrderImportApplication.class, args)));
    }
}
