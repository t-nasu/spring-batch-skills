package com.example.salesbatch.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額を表す値オブジェクト。小数第 2 位までで保持する。
 *
 * <p>返品を差し引いた結果として合計が負になることはありうるため、
 * この型自体は負値を許容する。個々の取引明細の金額が負でないことは
 * {@link RawSalesTransaction} の検証で担保する。</p>
 */
public record Money(BigDecimal value) implements Comparable<Money> {

    /** DB の {@code DECIMAL(..,2)} に合わせた小数桁数。 */
    public static final int SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(value, "value は必須です");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money add(Money other) {
        return new Money(value.add(other.value));
    }

    public Money subtract(Money other) {
        return new Money(value.subtract(other.value));
    }

    public Money negate() {
        return new Money(value.negate());
    }

    /** 符号（+1 / -1）を掛けた金額。 */
    public Money multipliedBy(int factor) {
        return new Money(value.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
