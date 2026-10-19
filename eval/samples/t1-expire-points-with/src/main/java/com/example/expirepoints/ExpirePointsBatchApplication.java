package com.example.expirepoints;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * expirePointsJob の起動クラス。
 *
 * <p>web サーバは起動しない（{@code spring.main.web-application-type=none}）。処理の成否は
 * 終了コードで返し、スケジューラに判定させる。
 */
@SpringBootApplication
public class ExpirePointsBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ExpirePointsBatchApplication.class, args)));
    }
}
