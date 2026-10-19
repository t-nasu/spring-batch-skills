package com.example.orderimport.infrastructure.file;

import java.util.List;

/**
 * 取引先から届く注文 CSV の書式。
 *
 * <p>UTF-8、ヘッダ行あり、カンマ区切り。列の並びは仕様書
 * {@code docs/batch-specs/t3-order-import.md} の「3. 入力」に従う。
 */
public final class OrderCsvFormat {

    /** ヘッダ行に現れる列名。CSV の並び順と一致している必要がある。 */
    public static final List<String> HEADERS =
            List.of("注文番号", "顧客コード", "商品コード", "数量", "単価", "注文日時");

    /**
     * {@code FieldSet} から値を取り出すための内部名。
     *
     * <p>{@link #HEADERS} と同じ並びで、位置で対応づけている。
     */
    public static final String[] FIELD_NAMES =
            {"orderNumber", "customerCode", "productCode", "quantity", "unitPrice", "orderedAt"};

    public static final String DELIMITER = ",";

    /** 本文が始まる前に読み飛ばす行数。 */
    public static final int HEADER_LINES = 1;

    private OrderCsvFormat() {
    }
}
