package com.example.salesbatch.domain.model;

import java.util.Locale;

/**
 * 取引区分。返品（{@link #RETURN}）は集計時に金額・数量を差し引く。
 */
public enum TransactionType {

    /** 売上。 */
    SALE,

    /** 返品。金額・数量を差し引く。 */
    RETURN;

    /**
     * テーブルに格納されたコード値から取引区分を解決する。
     *
     * @throws IllegalArgumentException 未知のコードの場合
     */
    public static TransactionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("取引区分が未設定です");
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("取引区分が不正です: " + code, e);
        }
    }

    /** 集計時に加算する符号（売上 = +1、返品 = -1）。 */
    public int sign() {
        return this == RETURN ? -1 : 1;
    }

    public boolean isReturn() {
        return this == RETURN;
    }
}
