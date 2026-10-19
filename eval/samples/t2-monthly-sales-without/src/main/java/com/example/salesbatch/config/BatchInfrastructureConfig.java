package com.example.salesbatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch の実行基盤の構成。
 *
 * <p>ジョブの実行履歴（{@code BATCH_*} 表）を DB に残すため、{@link JobRepository} を
 * JDBC 版に差し替える。Spring Batch 6 の既定は <strong>メモリ上にしか履歴を残さない</strong>
 * {@code ResourcelessJobRepository} であり、そのままでは次の 2 つの要件を満たせない。</p>
 *
 * <ul>
 *   <li>失敗した箇所からの再開（前回どこまで処理したかを DB に残す必要がある）</li>
 *   <li>同一対象年月の二重起動の防止（実行済みのジョブインスタンスを DB で判定する）</li>
 * </ul>
 *
 * <p>このクラスが {@link DefaultBatchConfiguration} を継承していることで、
 * Spring Boot の {@code BatchAutoConfiguration}（既定の構成）は適用されなくなる。</p>
 */
@Configuration
public class BatchInfrastructureConfig extends DefaultBatchConfiguration {

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public BatchInfrastructureConfig(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Override
    public JobRepository jobRepository() {
        JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        try {
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception e) {
            throw new BatchConfigurationException("JobRepository を初期化できませんでした", e);
        }
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
