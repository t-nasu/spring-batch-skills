package com.example.monthlysales.domain.shared;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * 集計の対象となる年月。
 *
 * <p>取引明細を絞り込む期間と、月次売上表の行を識別する項目の両方に使う。
 */
public record TargetMonth(YearMonth value) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuuMM");

    public TargetMonth {
        Objects.requireNonNull(value, "対象年月が無い");
    }

    /** {@code yyyyMM} 形式の文字列から組み立てる。 */
    public static TargetMonth parse(String yyyyMM) {
        if (yyyyMM == null || !yyyyMM.matches("\\d{6}")) {
            throw new IllegalArgumentException("対象年月は yyyyMM 形式で指定する: " + yyyyMM);
        }
        try {
            return new TargetMonth(YearMonth.parse(yyyyMM, FORMAT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("対象年月として解釈できない: " + yyyyMM, e);
        }
    }

    public static TargetMonth of(YearMonth value) {
        return new TargetMonth(value);
    }

    /** 対象年月の初日。取引日の下限（以上）として使う。 */
    public LocalDate firstDay() {
        return value.atDay(1);
    }

    /** 翌月の初日。取引日の上限（未満）として使う。 */
    public LocalDate firstDayOfNextMonth() {
        return value.plusMonths(1).atDay(1);
    }

    /** 指定した日が対象年月に含まれるか。 */
    public boolean contains(LocalDate date) {
        return date != null && YearMonth.from(date).equals(value);
    }

    /** {@code yyyyMM} 形式の文字列。月次売上表に格納する値でもある。 */
    public String format() {
        return value.format(FORMAT);
    }

    @Override
    public String toString() {
        return format();
    }
}
