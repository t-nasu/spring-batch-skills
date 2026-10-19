package com.example.pointbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * バッチアプリケーションの起動クラス。
 *
 * <p>{@code SpringApplication.exit} の戻り値をそのまま終了コードとして返すことで、
 * スケジューラがジョブの成否を判定できるようにする。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PointBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PointBatchApplication.class, args)));
    }
}
