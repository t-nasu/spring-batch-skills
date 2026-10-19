package com.example.salesbatch.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TargetMonthTest {

    @Nested
    @DisplayName("yyyyMM 形式の文字列からの生成")
    class Of {

        @Test
        void 対象年月を解釈できる() {
            TargetMonth targetMonth = TargetMonth.of("202509");

            assertThat(targetMonth.asString()).isEqualTo("202509");
            assertThat(targetMonth.firstDate()).isEqualTo(LocalDate.of(2025, 9, 1));
            assertThat(targetMonth.firstDateOfNextMonth()).isEqualTo(LocalDate.of(2025, 10, 1));
        }

        @Test
        void 年をまたぐ場合も翌月初日を返す() {
            assertThat(TargetMonth.of("202512").firstDateOfNextMonth()).isEqualTo(LocalDate.of(2026, 1, 1));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "2025", "2025-09", "20259", "2025099", "202513", "202500", "20xx09"})
        void 形式が不正なら例外になる(String value) {
            assertThatThrownBy(() -> TargetMonth.of(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("取引日の範囲判定")
    class Contains {

        private final TargetMonth targetMonth = TargetMonth.of("202502");

        @Test
        void 月初日と月末日は範囲内() {
            assertThat(targetMonth.contains(LocalDate.of(2025, 2, 1))).isTrue();
            assertThat(targetMonth.contains(LocalDate.of(2025, 2, 28))).isTrue();
        }

        @Test
        void 前月末日と翌月初日は範囲外() {
            assertThat(targetMonth.contains(LocalDate.of(2025, 1, 31))).isFalse();
            assertThat(targetMonth.contains(LocalDate.of(2025, 3, 1))).isFalse();
        }
    }
}
