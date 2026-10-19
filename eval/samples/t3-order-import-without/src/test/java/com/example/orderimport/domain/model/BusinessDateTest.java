package com.example.orderimport.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BusinessDateTest {

    @Test
    @DisplayName("yyyyMMdd 形式を解釈する")
    void 解釈できる() {
        assertThat(BusinessDate.parse("20260118").value()).isEqualTo(LocalDate.of(2026, 1, 18));
    }

    @Test
    @DisplayName("文字列表現は yyyyMMdd に戻る")
    void 文字列表現が往復する() {
        assertThat(BusinessDate.parse("20260118")).hasToString("20260118");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "2026-01-18", "20260132", "260118", "yyyyMMdd"})
    @DisplayName("書式が違う値は拒否する")
    void 不正な書式を拒否する(String raw) {
        assertThatThrownBy(() -> BusinessDate.parse(raw))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
