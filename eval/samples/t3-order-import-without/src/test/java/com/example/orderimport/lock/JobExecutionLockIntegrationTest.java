package com.example.orderimport.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.application.lock.JobExecutionLockService;
import com.example.orderimport.application.port.JobLockStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 二重起動防止ロックの検証。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:order_import_lock;MODE=MySQL;DB_CLOSE_DELAY=-1")
class JobExecutionLockIntegrationTest {

    private static final String JOB_NAME = "orderImportJob";
    private static final String OTHER_HOST = "other-host:9999";

    @Autowired
    JobExecutionLockService lockService;

    @Autowired
    JobLockStore lockStore;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLocks() {
        jdbcTemplate.update("DELETE FROM batch_job_lock");
    }

    @Test
    @DisplayName("ロックを保持している間は、同じジョブを起動できない")
    void 二重起動を防ぐ() {
        assertThat(lockService.tryAcquire(JOB_NAME)).isTrue();

        assertThat(lockService.tryAcquire(JOB_NAME))
                .as("既に自分が保持していても、二重に獲得はできない")
                .isFalse();
        assertThat(lockStore.acquire(JOB_NAME, OTHER_HOST, LocalDateTime.now()))
                .as("別プロセスからも獲得できない")
                .isFalse();
    }

    @Test
    @DisplayName("解放すれば次の起動が通る")
    void 解放すれば再取得できる() {
        assertThat(lockService.tryAcquire(JOB_NAME)).isTrue();
        lockService.release(JOB_NAME);

        assertThat(lockStore.find(JOB_NAME)).isEmpty();
        assertThat(lockService.tryAcquire(JOB_NAME)).isTrue();
    }

    @Test
    @DisplayName("他プロセスが保持しているロックは解放できない")
    void 他者のロックは解放しない() {
        assertThat(lockStore.acquire(JOB_NAME, OTHER_HOST, LocalDateTime.now())).isTrue();

        lockService.release(JOB_NAME);

        assertThat(lockStore.find(JOB_NAME))
                .as("自分が獲得していないロックを消してしまわないこと")
                .isPresent()
                .get()
                .extracting(JobLockStore.JobLock::ownerId)
                .isEqualTo(OTHER_HOST);
    }

    @Test
    @DisplayName("異常終了で取り残された古いロックは、次の起動時に引き継ぐ")
    void 古いロックを引き継ぐ() {
        // 前回の実行がロックを残したまま落ちた状況を作る（既定の猶予は 6 時間）。
        LocalDateTime staleLockedAt = LocalDateTime.now().minusHours(7);
        assertThat(lockStore.acquire(JOB_NAME, OTHER_HOST, staleLockedAt)).isTrue();

        assertThat(lockService.tryAcquire(JOB_NAME)).isTrue();
        assertThat(lockStore.find(JOB_NAME))
                .get()
                .extracting(JobLockStore.JobLock::ownerId)
                .isEqualTo(lockService.ownerId());
    }

    @Test
    @DisplayName("まだ猶予内のロックは引き継がない")
    void 猶予内のロックは奪わない() {
        LocalDateTime recentLockedAt = LocalDateTime.now().minusHours(1);
        assertThat(lockStore.acquire(JOB_NAME, OTHER_HOST, recentLockedAt)).isTrue();

        assertThat(lockService.tryAcquire(JOB_NAME))
                .as("実行中かもしれないロックを奪うと二重起動になる")
                .isFalse();
        assertThat(lockStore.find(JOB_NAME))
                .get()
                .extracting(JobLockStore.JobLock::ownerId)
                .isEqualTo(OTHER_HOST);
    }
}
