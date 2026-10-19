package com.example.monthlysales.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TargetMonthTest {

    @Test
    void 月初日と翌月初日で取引日の範囲を表す() {
        TargetMonth targetMonth = TargetMonth.parse("202509");

        assertThat(targetMonth.firstDay()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(targetMonth.firstDayOfNextMonth()).isEqualTo(LocalDate.of(2025, 10, 1));
    }

    @Test
    void 年をまたぐ月でも翌月初日を正しく求める() {
        assertThat(TargetMonth.parse("202512").firstDayOfNextMonth())
                .isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void 対象年月に含まれるかを判定する() {
        TargetMonth targetMonth = TargetMonth.parse("202509");

        assertThat(targetMonth.contains(LocalDate.of(2025, 9, 1))).isTrue();
        assertThat(targetMonth.contains(LocalDate.of(2025, 9, 30))).isTrue();
        assertThat(targetMonth.contains(LocalDate.of(2025, 8, 31))).isFalse();
        assertThat(targetMonth.contains(LocalDate.of(2025, 10, 1))).isFalse();
        assertThat(targetMonth.contains(null)).isFalse();
    }

    @Test
    void 表に格納する形はyyyyMMである() {
        assertThat(TargetMonth.parse("202509").format()).isEqualTo("202509");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "2025-09", "20259", "2025099", "202513", "yyyyMM"})
    void 形式が違う対象年月は受け付けない(String value) {
        assertThatThrownBy(() -> TargetMonth.parse(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
