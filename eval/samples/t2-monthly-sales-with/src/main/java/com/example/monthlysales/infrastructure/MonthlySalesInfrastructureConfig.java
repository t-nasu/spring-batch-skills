package com.example.monthlysales.infrastructure;

import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryMerger;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryRepository;
import com.example.monthlysales.infrastructure.sales.SalesTransactionItemReader;
import com.example.monthlysales.infrastructure.summary.MonthlySalesSummaryItemWriter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 入出力まわりの Bean 定義。
 *
 * <p>Job の定義（batch 層）へはここで組み立てたものを Spring Batch の型で渡し、
 * batch 層が infrastructure のクラスを直に扱わずに済むようにする。
 */
@Configuration
public class MonthlySalesInfrastructureConfig {

    /** ドメインサービス。domain を Spring に依存させないため、ここで組み立てる。 */
    @Bean
    public MonthlySalesSummaryMerger monthlySalesSummaryMerger() {
        return new MonthlySalesSummaryMerger();
    }

    @Bean
    @StepScope
    public SalesTransactionItemReader salesTransactionItemReader(
            SqlSessionFactory sqlSessionFactory,
            @Value("#{jobParameters['targetMonth']}") String targetMonth) {
        return new SalesTransactionItemReader(sqlSessionFactory, TargetMonth.parse(targetMonth));
    }

    @Bean
    public MonthlySalesSummaryItemWriter monthlySalesSummaryItemWriter(
            MonthlySalesSummaryRepository repository, MonthlySalesSummaryMerger merger) {
        return new MonthlySalesSummaryItemWriter(repository, merger);
    }
}
