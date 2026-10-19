package com.example.pointbatch.domain.model.shared;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;

/**
 * 業務日付を表す値オブジェクト。
 *
 * <p>起動引数 {@code targetDate=yyyyMMdd} に対応する。
 */
public record BusinessDate(LocalDate value) {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    public BusinessDate {
        Objects.requireNonNull(value, "業務日付は必須です");
    }

    /**
     * {@code yyyyMMdd} 形式の文字列から生成する。
     *
     * @throws InvalidPointDataException 形式不正、または存在しない日付の場合
     */
    public static BusinessDate parse(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            throw new InvalidPointDataException("業務日付が指定されていません");
        }
        try {
            return new BusinessDate(LocalDate.parse(yyyyMMdd, FORMATTER));
        } catch (DateTimeException e) {
            throw new InvalidPointDataException(
                    "業務日付の形式が不正です(yyyyMMdd 形式で指定してください): " + yyyyMMdd);
        }
    }

    public static BusinessDate of(LocalDate value) {
        return new BusinessDate(value);
    }

    @Override
    public String toString() {
        return value.format(FORMATTER);
    }
}
