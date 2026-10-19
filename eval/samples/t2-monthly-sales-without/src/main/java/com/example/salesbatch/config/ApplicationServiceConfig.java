package com.example.salesbatch.config;

import com.example.salesbatch.application.service.MonthlySalesGroupLoaderFactory;
import com.example.salesbatch.application.service.MonthlySalesSummaryRegistrationService;
import com.example.salesbatch.domain.repository.MonthlySalesSummaryRepository;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * アプリケーション層の組み立て。
 *
 * <p>アプリケーション層・ドメイン層のクラスはフレームワークに依存させたくないため、
 * アノテーションではなくこの構成クラスで Bean として登録する。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MonthlySalesBatchProperties.class)
public class ApplicationServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public MonthlySalesGroupLoaderFactory monthlySalesGroupLoaderFactory(
            SalesTransactionRepository salesTransactionRepository, MonthlySalesBatchProperties properties) {
        return new MonthlySalesGroupLoaderFactory(
                salesTransactionRepository, properties.pageSize(), properties.skipLimit());
    }

    @Bean
    public MonthlySalesSummaryRegistrationService monthlySalesSummaryRegistrationService(
            MonthlySalesSummaryRepository monthlySalesSummaryRepository) {
        return new MonthlySalesSummaryRegistrationService(monthlySalesSummaryRepository);
    }
}
