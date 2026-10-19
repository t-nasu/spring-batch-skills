package com.example.orderimport.domain.model.order;

/**
 * 商品コード。
 *
 * <p>書式として妥当であることだけをこのクラスで保証する。商品マスタに実在するか
 * どうかは {@link com.example.orderimport.domain.service.OrderValidationService}
 * が判定する。
 */
public record ProductCode(String value) {

    private static final int MAX_LENGTH = 20;

    public ProductCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("商品コードが空です");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "商品コードは " + MAX_LENGTH + " 文字以内で指定してください: " + value);
        }
    }

    public static ProductCode of(String value) {
        return new ProductCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
