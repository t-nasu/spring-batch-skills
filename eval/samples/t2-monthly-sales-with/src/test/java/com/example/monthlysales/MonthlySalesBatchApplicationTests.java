package com.example.monthlysales;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monthlysales.batch.MonthlySalesSummaryJobConfig;
import com.example.monthlysales.infrastructure.sales.SalesTransactionMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Bean の組み立てと Mapper XML の読み込みが通ることを確かめる。
 *
 * <p>MySQL 専用の SQL は実行できないため、ここでは文の中身までは確かめない。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:monthly-sales;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.batch.jdbc.initialize-schema=always",
        // 組み立てだけを見たいので、起動と同時にジョブが走らないようにする
        "spring.batch.job.enabled=false"
})
class MonthlySalesBatchApplicationTests {

    @Autowired
    private Job monthlySalesSummaryJob;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void ジョブが組み立てられ名前と再開可否が仕様どおりである() {
        assertThat(monthlySalesSummaryJob.getName())
                .isEqualTo(MonthlySalesSummaryJobConfig.JOB_NAME);
        assertThat(monthlySalesSummaryJob.isRestartable()).isTrue();
    }

    /** XML が読み込まれていないと、文は実行時まで見つからない。ここで先に気づけるようにする。 */
    @Test
    void MapperのXMLが読み込まれ文が登録されている() {
        var configuration = sqlSessionFactory.getConfiguration();

        assertThat(configuration.hasStatement(
                SalesTransactionMapper.SELECT_BY_TRANSACTION_DATE_RANGE)).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.monthlysales.infrastructure.summary.MonthlySalesSummaryMapper.addAll"))
                .isTrue();
    }
}
