package com.example.orderimport.domain.model.order;

import java.util.regex.Pattern;

/**
 * 注文数量。
 *
 * <p>仕様により 1 以上でなければならない（0 や負数は不正データとして扱う）。
 */
public record Quantity(int value) {

    /**
     * 受け付ける表記。
     *
     * <p>{@code Integer.parseInt} は全角数字（{@code "１０"}）も解釈してしまう。
     * 単価は {@code BigDecimal} が全角を弾くため、放っておくと数量だけが通るという
     * ちぐはぐな挙動になる。取引先に差し戻すべき表記ゆれを黙って受け入れないよう、
     * 半角数字だけに絞る。
     */
    private static final Pattern ASCII_INTEGER = Pattern.compile("[+-]?[0-9]+");

    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("数量は 1 以上で指定してください: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    /**
     * {@code "12"} のような文字列から数量を生成する。
     *
     * @throws IllegalArgumentException 整数として解釈できない、または 1 未満の場合
     */
    public static Quantity parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("数量が空です");
        }
        String trimmed = raw.trim();
        if (!ASCII_INTEGER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("数量が整数ではありません: " + raw);
        }
        int parsed;
        try {
            parsed = Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // 桁数が int に収まらない場合。
            throw new IllegalArgumentException("数量が大きすぎます: " + raw, e);
        }
        return new Quantity(parsed);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
