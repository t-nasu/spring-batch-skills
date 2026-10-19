package com.example.orderimport.domain.customer;

/**
 * 顧客コード。
 */
public record CustomerCode(String value) {

    public CustomerCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("顧客コードは必須である");
        }
        value = value.trim();
    }

    public static CustomerCode of(String value) {
        return new CustomerCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
