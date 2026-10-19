package com.example.orderimport.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Nested
    @DisplayName("文字列からの生成")
    class Parse {

        @Test
        @DisplayName("整数表記も小数表記も小数第 2 位に揃えて保持する")
        void 小数第2位に正規化される() {
            assertThat(Money.parse("100").value()).isEqualByComparingTo("100.00");
            assertThat(Money.parse("1980.5").value()).isEqualByComparingTo("1980.50");
            assertThat(Money.parse("1980.50").value()).isEqualByComparingTo("1980.50");
        }

        @Test
        @DisplayName("桁数が違っても同じ金額なら等しい")
        void 桁数が違っても等価になる() {
            assertThat(Money.parse("100")).isEqualTo(Money.parse("100.00"));
        }

        @Test
        @DisplayName("前後の空白は無視する")
        void 空白を取り除く() {
            assertThat(Money.parse("  250.00 ")).isEqualTo(Money.of(250));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "abc", "1,000", "100.123", "-1"})
        @DisplayName("数値でない・負数・小数第 3 位以下を持つ値は拒否する")
        void 不正な値を拒否する(String raw) {
            assertThatThrownBy(() -> Money.parse(raw))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null は拒否する")
        void nullを拒否する() {
            assertThatThrownBy(() -> Money.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("乗算は金額 × 個数を返す")
    void 乗算できる() {
        assertThat(Money.parse("1980.50").multipliedBy(3)).isEqualTo(Money.parse("5941.50"));
    }

    @Test
    @DisplayName("加算は合計を返す")
    void 加算できる() {
        assertThat(Money.parse("100.25").plus(Money.parse("200.75"))).isEqualTo(Money.parse("301.00"));
    }

    @Test
    @DisplayName("0 は許容する")
    void ゼロを許容する() {
        assertThat(Money.of(BigDecimal.ZERO)).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("負の金額は作れない")
    void 負数を拒否する() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
