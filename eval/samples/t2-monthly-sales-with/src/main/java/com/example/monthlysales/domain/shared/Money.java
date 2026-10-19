package com.example.monthlysales.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額。
 *
 * <p>小数第 2 位に揃えて保持する。月次売上表の金額欄も同じ位取りである。
 * 集計結果は返品が売上を上回ると負になりうるため、ここでは符号を制限しない。
 */
public record Money(BigDecimal value) {

    /** 金額の位取り。DB の DECIMAL(_, 2) に合わせる。 */
    private static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(value, "金額が無い");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(value.add(other.value));
    }

    /** 符号を反転した金額。返品を差し引くときに使う。 */
    public Money negate() {
        return new Money(value.negate());
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
