package com.example.salesbatch.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 集計対象年月を表す値オブジェクト。
 *
 * <p>バッチの起動引数 {@code targetMonth=yyyyMM} と、抽出条件である取引日の範囲
 * （月初日以上、翌月初日未満）の対応づけをこのクラスに集約する。</p>
 */
public record TargetMonth(YearMonth value) implements Comparable<TargetMonth> {

    /** 起動引数および {@code monthly_sales_summary.target_month} の書式。 */
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMM").withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern PATTERN = Pattern.compile("\\d{6}");

    public TargetMonth {
        Objects.requireNonNull(value, "value は必須です");
    }

    /**
     * {@code yyyyMM} 形式の文字列から生成する。
     *
     * @throws IllegalArgumentException 形式が不正な場合
     */
    public static TargetMonth of(String yyyyMM) {
        if (yyyyMM == null || !PATTERN.matcher(yyyyMM).matches()) {
            throw new IllegalArgumentException(
                    "対象年月は yyyyMM 形式（半角数字 6 桁）で指定してください: " + yyyyMM);
        }
        try {
            return new TargetMonth(YearMonth.parse(yyyyMM, FORMATTER));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("対象年月として解釈できません: " + yyyyMM, e);
        }
    }

    public static TargetMonth of(YearMonth yearMonth) {
        return new TargetMonth(yearMonth);
    }

    /** {@code yyyyMM} 形式の文字列表現。 */
    public String asString() {
        return value.format(FORMATTER);
    }

    /** 対象年月の初日（抽出条件の下限、この日を含む）。 */
    public LocalDate firstDate() {
        return value.atDay(1);
    }

    /** 翌月の初日（抽出条件の上限、この日を含まない）。 */
    public LocalDate firstDateOfNextMonth() {
        return value.plusMonths(1).atDay(1);
    }

    /** 指定日が対象年月の範囲内かどうか。 */
    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(firstDate()) && date.isBefore(firstDateOfNextMonth());
    }

    @Override
    public int compareTo(TargetMonth other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return asString();
    }
}
