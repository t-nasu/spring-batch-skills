package com.example.orderimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 注文取り込みバッチの起動クラス。
 *
 * <p>常駐せず、ジョブを 1 回実行して終了する。処理結果は終了コードで表現する
 * （{@link com.example.orderimport.presentation.runner.JobExitCode} を参照）。
 */
@SpringBootApplication
public class OrderImportBatchApplication {

    public static void main(String[] args) {
        // SpringApplication.exit() がランナーの ExitCodeGenerator を拾い、
        // ジョブの結果を終了コードに反映する。
        System.exit(SpringApplication.exit(
                SpringApplication.run(OrderImportBatchApplication.class, args)));
    }
}
