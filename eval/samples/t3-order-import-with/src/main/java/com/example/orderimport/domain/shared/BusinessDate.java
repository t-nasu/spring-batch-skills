package com.example.orderimport.domain.shared;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 業務日付。
 */
public record BusinessDate(LocalDate value) {

    public BusinessDate {
        Objects.requireNonNull(value, "業務日付は必須である");
    }

    public static BusinessDate of(LocalDate value) {
        return new BusinessDate(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
