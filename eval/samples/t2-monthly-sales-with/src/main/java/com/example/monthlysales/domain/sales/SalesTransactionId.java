package com.example.monthlysales.domain.sales;

/** 取引明細を識別する値。スキップしたときにログへ出す ID でもある。 */
public record SalesTransactionId(String value) {

    public SalesTransactionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("取引 ID が空である");
        }
    }

    public static SalesTransactionId of(String value) {
        return new SalesTransactionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
