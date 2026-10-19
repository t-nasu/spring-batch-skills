package com.example.salesbatch.presentation.batch.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

class DeadlockRetryPolicyTest {

    @Test
    void MySQLのデッドロックは再試行する() {
        SQLException deadlock = new SQLException("Deadlock found when trying to get lock", "40001", 1213);

        assertThat(DeadlockRetryPolicy.isDeadlock(new CannotAcquireLockException("write failed", deadlock))).isTrue();
    }

    @Test
    void エラーコードだけ一致する場合も再試行する() {
        SQLException deadlock = new SQLException("Deadlock found", "HY000", 1213);

        assertThat(DeadlockRetryPolicy.isDeadlock(deadlock)).isTrue();
    }

    @Test
    void 原因例外が何段か深くてもデッドロックを見つける() {
        Throwable nested = new IllegalStateException("wrapped",
                new RuntimeException("mybatis", new SQLTransactionRollbackException("deadlock", "40001", 1213)));

        assertThat(DeadlockRetryPolicy.isDeadlock(nested)).isTrue();
    }

    @Test
    void ロック待ちタイムアウトは再試行しない() {
        SQLException lockWaitTimeout =
                new SQLException("Lock wait timeout exceeded", "HY000", 1205);

        assertThat(DeadlockRetryPolicy.isDeadlock(new CannotAcquireLockException("write failed", lockWaitTimeout)))
                .isFalse();
    }

    @Test
    void デッドロック以外のデータアクセス例外は再試行しない() {
        assertThat(DeadlockRetryPolicy.isDeadlock(
                new DataIntegrityViolationException("duplicate", new SQLException("dup", "23000", 1062)))).isFalse();
        assertThat(DeadlockRetryPolicy.isDeadlock(new QueryTimeoutException("timeout"))).isFalse();
        assertThat(DeadlockRetryPolicy.isDeadlock(new IllegalStateException("unrelated"))).isFalse();
    }

    @Test
    void 原因例外が循環していても判定が終わる() {
        RuntimeException first = new RuntimeException("first");
        assertThat(DeadlockRetryPolicy.isDeadlock(new RuntimeException("second", first))).isFalse();
    }

    @Test
    void 生成したポリシーはデッドロックのみ再試行対象とする() {
        RetryPolicy policy = DeadlockRetryPolicy.create(3, Duration.ofMillis(10), Duration.ofMillis(100));

        assertThat(policy.shouldRetry(new CannotAcquireLockException("deadlock",
                new SQLException("Deadlock found", "40001", 1213)))).isTrue();
        assertThat(policy.shouldRetry(new IllegalStateException("other"))).isFalse();
    }
}
