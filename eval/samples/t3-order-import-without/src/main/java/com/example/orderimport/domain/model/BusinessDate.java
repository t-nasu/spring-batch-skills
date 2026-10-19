package com.example.orderimport.domain.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * 業務日付。
 *
 * <p>ジョブ引数 {@code targetDate} で指定された日付であり、取り込んだ注文の帰属日
 * および日次売上の集計キーになる。注文日時（{@code ordered_at}）とは独立しており、
 * ファイルの締め時刻がずれても集計対象が変わらないようにするための概念。
 */
public record BusinessDate(LocalDate value) {

    /** ジョブ引数で受け取るときの書式。 */
    public static final DateTimeFormatter PARAM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public BusinessDate {
        Objects.requireNonNull(value, "業務日付は必須です");
    }

    /**
     * {@code yyyyMMdd} 形式の文字列から業務日付を生成する。
     *
     * @throws IllegalArgumentException 書式が不正な場合
     */
    public static BusinessDate parse(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
            throw new IllegalArgumentException("業務日付が空です");
        }
        try {
            return new BusinessDate(LocalDate.parse(yyyyMMdd.trim(), PARAM_FORMAT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "業務日付は yyyyMMdd 形式で指定してください: " + yyyyMMdd, e);
        }
    }

    public static BusinessDate of(LocalDate date) {
        return new BusinessDate(date);
    }

    @Override
    public String toString() {
        return value.format(PARAM_FORMAT);
    }
}
