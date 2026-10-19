package com.example.salesbatch.presentation.batch.retry;

import java.sql.SQLException;
import java.time.Duration;

import org.springframework.core.retry.RetryPolicy;

/**
 * デッドロックのときだけ再試行するリトライポリシー。
 *
 * <p>仕様上、再試行するのは DB のデッドロックのみ。デッドロックが Spring のどの例外型に
 * 変換されるかは例外変換の実装に依存する（{@code CannotAcquireLockException} や
 * {@code PessimisticLockingFailureException} など）ため、例外の型では判断せず、
 * 原因例外をたどって MySQL のデッドロック（エラーコード 1213 / SQLState 40001）かどうかを確かめる。</p>
 *
 * <p>ロック待ちタイムアウト（エラーコード 1205、SQLState HY000）はデッドロックではないため
 * 再試行の対象にしない。</p>
 */
public final class DeadlockRetryPolicy {

    /** MySQL のデッドロック時のエラーコード。 */
    private static final int MYSQL_ERROR_CODE_DEADLOCK = 1213;

    /** デッドロックによるロールバックを表す SQLState（H2 も同じ値を返す）。 */
    private static final String SQL_STATE_SERIALIZATION_FAILURE = "40001";

    /** 原因例外の循環参照に備えた、たどる段数の上限。 */
    private static final int MAX_CAUSE_DEPTH = 20;

    private DeadlockRetryPolicy() {
    }

    /**
     * デッドロック時のみ再試行するポリシーを生成する。
     *
     * @param maxRetries  再試行回数（初回実行は含まない）
     * @param delay       初回の待ち時間
     * @param maxDelay    待ち時間の上限（待ち時間は 2 倍ずつ伸ばす）
     */
    public static RetryPolicy create(int maxRetries, Duration delay, Duration maxDelay) {
        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(delay)
                .multiplier(2.0)
                .maxDelay(maxDelay)
                .predicate(DeadlockRetryPolicy::isDeadlock)
                .build();
    }

    /** 例外（およびその原因）がデッドロックによるものか。 */
    public static boolean isDeadlock(Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof SQLException sqlException && isDeadlock(sqlException)) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    private static boolean isDeadlock(SQLException sqlException) {
        return sqlException.getErrorCode() == MYSQL_ERROR_CODE_DEADLOCK
                || SQL_STATE_SERIALIZATION_FAILURE.equals(sqlException.getSQLState());
    }
}
