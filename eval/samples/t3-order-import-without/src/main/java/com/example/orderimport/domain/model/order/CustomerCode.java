package com.example.orderimport.domain.model.order;

/**
 * 顧客コード。
 *
 * <p>書式として妥当であることだけをこのクラスで保証する。顧客マスタに実在するか
 * どうかは {@link com.example.orderimport.domain.service.OrderValidationService}
 * が判定する。
 */
public record CustomerCode(String value) {

    private static final int MAX_LENGTH = 20;

    public CustomerCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("顧客コードが空です");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "顧客コードは " + MAX_LENGTH + " 文字以内で指定してください: " + value);
        }
    }

    public static CustomerCode of(String value) {
        return new CustomerCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
