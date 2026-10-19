package com.example.orderimport.config;

import java.time.Clock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * アプリケーション全体の設定。
 */
@Configuration
@EnableConfigurationProperties(OrderImportProperties.class)
@MapperScan("com.example.orderimport.infrastructure.datasource.mapper")
public class ApplicationConfig {

    /**
     * 現在時刻の供給源。
     *
     * <p>{@code LocalDateTime.now()} を直接呼ばず Bean 経由にしているのは、
     * 登録日時やロックの獲得時刻をテストから固定できるようにするため。
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
