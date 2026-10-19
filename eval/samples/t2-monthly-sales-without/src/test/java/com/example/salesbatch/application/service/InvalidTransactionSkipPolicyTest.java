package com.example.salesbatch.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.salesbatch.application.exception.SkipLimitExceededException;
import com.example.salesbatch.domain.exception.InvalidSalesTransactionException;

import org.junit.jupiter.api.Test;

class InvalidTransactionSkipPolicyTest {

    private static InvalidSalesTransactionException invalid(long transactionId) {
        return new InvalidSalesTransactionException(transactionId, "数量が未設定です");
    }

    @Test
    void 上限までは読み飛ばせる() {
        InvalidTransactionSkipPolicy policy = new InvalidTransactionSkipPolicy(3);

        assertThatCode(() -> {
            policy.record(invalid(1L));
            policy.record(invalid(2L));
            policy.record(invalid(3L));
        }).doesNotThrowAnyException();

        assertThat(policy.skippedCount()).isEqualTo(3);
    }

    @Test
    void 上限を超えると例外になる() {
        InvalidTransactionSkipPolicy policy = new InvalidTransactionSkipPolicy(2);
        policy.record(invalid(1L));
        policy.record(invalid(2L));

        assertThatThrownBy(() -> policy.record(invalid(3L)))
                .isInstanceOf(SkipLimitExceededException.class)
                .hasMessageContaining("limit=2")
                .hasMessageContaining("skipped=3");
    }

    @Test
    void 同じ取引を二度記録しても一件として数える() {
        InvalidTransactionSkipPolicy policy = new InvalidTransactionSkipPolicy(1);

        policy.record(invalid(1L));
        assertThatCode(() -> policy.record(invalid(1L))).doesNotThrowAnyException();

        assertThat(policy.skippedCount()).isEqualTo(1);
    }

    @Test
    void 再開時は前回までの件数を引き継ぐ() {
        InvalidTransactionSkipPolicy policy = new InvalidTransactionSkipPolicy(10);
        policy.restore(9);

        policy.record(invalid(100L));
        assertThat(policy.skippedCount()).isEqualTo(10);

        assertThatThrownBy(() -> policy.record(invalid(101L)))
                .isInstanceOf(SkipLimitExceededException.class);
    }
}
