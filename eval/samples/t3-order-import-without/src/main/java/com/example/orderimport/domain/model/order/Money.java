package com.example.orderimport.domain.model.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額。
 *
 * <p>通貨は日本円のみを扱うため通貨単位は保持しない。小数第 2 位までに正規化して
 * 保持し、{@code equals} が桁数の違いに引きずられないようにしている
 * （{@code BigDecimal} は 100 と 100.00 を等しいとみなさないため）。
 */
public record Money(BigDecimal value) {

    /** DB の DECIMAL 定義に合わせたスケール。 */
    public static final int SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(value, "金額が空です");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("金額は 0 以上で指定してください: " + value);
        }
        value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    /**
     * {@code "1980.50"} のような文字列から金額を生成する。
     *
     * @throws IllegalArgumentException 数値として解釈できない、負数、または小数第 3 位
     *                                  以下の端数を持つ場合
     */
    public static Money parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("金額が空です");
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金額が数値ではありません: " + raw, e);
        }
        if (parsed.scale() > SCALE) {
            throw new IllegalArgumentException("金額は小数第 " + SCALE + " 位までで指定してください: " + raw);
        }
        return new Money(parsed);
    }

    public Money multipliedBy(int multiplier) {
        return new Money(value.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money plus(Money other) {
        return new Money(value.add(other.value));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
