package com.example.expirepoints.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BusinessDateTest {

    @Test
    void yyyyMMdd_を解釈する() {
        assertThat(BusinessDate.parse("20261019").value()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void 文字列に戻すと_yyyyMMdd_になる() {
        assertThat(BusinessDate.of(LocalDate.of(2026, 1, 5))).hasToString("20260105");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "2026-10-19", "20261332", "20260230", "261019", "202610190"})
    void 形式が合わない文字列は受け付けない(String value) {
        assertThatThrownBy(() -> BusinessDate.parse(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
