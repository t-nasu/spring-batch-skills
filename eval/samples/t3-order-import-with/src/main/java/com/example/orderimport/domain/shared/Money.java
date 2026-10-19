package com.example.orderimport.domain.shared;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金額。負の値は持たない。
 */
public record Money(BigDecimal value) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(value, "金額は必須である");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("金額に負の値は使えない: " + value);
        }
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public Money times(int count) {
        return new Money(value.multiply(BigDecimal.valueOf(count)));
    }

    public Money plus(Money other) {
        return new Money(value.add(other.value));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
