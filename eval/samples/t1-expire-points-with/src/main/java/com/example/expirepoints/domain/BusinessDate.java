package com.example.expirepoints.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;

/**
 * 業務日付。
 *
 * <p>このバッチでは、起動引数で渡された日付をその日の基準として扱う。
 */
public record BusinessDate(LocalDate value) {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    public BusinessDate {
        Objects.requireNonNull(value, "業務日付は必須である");
    }

    /**
     * {@code yyyyMMdd} 形式の文字列から業務日付を作る。
     *
     * @throws IllegalArgumentException 形式が合わない、または存在しない日付のとき
     */
    public static BusinessDate parse(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            throw new IllegalArgumentException("業務日付が指定されていない");
        }
        try {
            return new BusinessDate(LocalDate.parse(yyyyMMdd, FORMAT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "業務日付は yyyyMMdd 形式で指定する: " + yyyyMMdd, e);
        }
    }

    public static BusinessDate of(LocalDate value) {
        return new BusinessDate(value);
    }

    @Override
    public String toString() {
        return value.format(FORMAT);
    }
}
