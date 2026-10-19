package com.example.orderimport.domain.validation;

import java.util.Objects;

/**
 * 1 項目分の検証エラー。
 *
 * <p>1 行の中に複数の誤りがあっても運用チームが一度に把握できるよう、検証は
 * 最初のエラーで打ち切らずこの型のリストとして積み上げる。
 *
 * @param field   CSV 上の項目名（例: {@code 数量}）
 * @param message エラー内容
 */
public record ValidationError(String field, String message) {

    public ValidationError {
        Objects.requireNonNull(field, "項目名が空です");
        Objects.requireNonNull(message, "エラー内容が空です");
    }

    public static ValidationError of(String field, String message) {
        return new ValidationError(field, message);
    }

    @Override
    public String toString() {
        return field + ": " + message;
    }
}
