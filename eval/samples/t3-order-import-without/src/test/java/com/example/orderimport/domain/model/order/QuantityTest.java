package com.example.orderimport.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuantityTest {

    @Test
    @DisplayName("1 以上の整数を受け付ける")
    void 正の整数を受け付ける() {
        assertThat(Quantity.parse("1").value()).isEqualTo(1);
        assertThat(Quantity.parse(" 250 ").value()).isEqualTo(250);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100"})
    @DisplayName("仕様どおり 1 未満は不正とする")
    void 一未満を拒否する(String raw) {
        assertThatThrownBy(() -> Quantity.parse(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数量は 1 以上");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "abc", "1.5", "１０"})
    @DisplayName("整数として読めない値は拒否する")
    void 整数でない値を拒否する(String raw) {
        assertThatThrownBy(() -> Quantity.parse(raw))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
