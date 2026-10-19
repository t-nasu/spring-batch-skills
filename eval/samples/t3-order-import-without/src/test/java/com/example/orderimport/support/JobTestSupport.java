package com.example.orderimport.support;

import com.example.orderimport.presentation.job.JobParameterNames;
import java.nio.file.Path;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * ジョブを実際に動かすテストの共通部分。
 *
 * <p>本番と同じ DDL を H2 の MySQL 互換モードに流して検証する。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestNotificationConfig.class)
public abstract class JobTestSupport {

    @Autowired
    protected JobOperator jobOperator;

    @Autowired
    protected Job orderImportJob;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RecordingNotificationGateway notificationGateway;

    /**
     * ジョブを起動する。
     *
     * <p>引数の組み立て方は {@code OrderImportJobRunner} と同じにしてある
     * （targetDate だけが識別引数）。同じ業務日付で呼び直せば再開になる。
     */
    protected JobExecution launchJob(String targetDate, Path inputFile) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString(JobParameterNames.TARGET_DATE, targetDate, true)
                .addString(JobParameterNames.INPUT_FILE, inputFile.toString(), false)
                .toJobParameters();
        return jobOperator.start(orderImportJob, parameters);
    }

    protected long countOrders(String businessDate) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE business_date = ?", Long.class, toSqlDate(businessDate));
        return count == null ? 0L : count;
    }

    protected long countDailySales(String businessDate) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM daily_sales WHERE business_date = ?", Long.class, toSqlDate(businessDate));
        return count == null ? 0L : count;
    }

    private static String toSqlDate(String yyyyMMdd) {
        return "%s-%s-%s".formatted(yyyyMMdd.substring(0, 4), yyyyMMdd.substring(4, 6), yyyyMMdd.substring(6, 8));
    }
}
