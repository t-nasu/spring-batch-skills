package com.example.orderimport.support;

import com.example.orderimport.infrastructure.file.OrderCsvFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * テスト用の注文 CSV を組み立てる。
 */
public final class OrderCsvFixture {

    public static final String HEADER = String.join(OrderCsvFormat.DELIMITER, OrderCsvFormat.HEADERS);

    private final List<String> lines = new ArrayList<>();

    private OrderCsvFixture(String header) {
        lines.add(header);
    }

    public static OrderCsvFixture withStandardHeader() {
        return new OrderCsvFixture(HEADER);
    }

    public static OrderCsvFixture withHeader(String header) {
        return new OrderCsvFixture(header);
    }

    public OrderCsvFixture row(String orderNumber, String customerCode, String productCode,
                               String quantity, String unitPrice, String orderedAt) {
        lines.add(String.join(OrderCsvFormat.DELIMITER,
                orderNumber, customerCode, productCode, quantity, unitPrice, orderedAt));
        return this;
    }

    /** 列数が合わないなど、整形されていない行をそのまま足す。 */
    public OrderCsvFixture rawLine(String line) {
        lines.add(line);
        return this;
    }

    /** 検証を通る行を連番で足す。 */
    public OrderCsvFixture validRows(int count, String orderNumberPrefix) {
        for (int i = 1; i <= count; i++) {
            row("%s%04d".formatted(orderNumberPrefix, i), "C001", "P001",
                    "1", "100.00", "2026-01-18 10:00:00");
        }
        return this;
    }

    /** 顧客コードがマスタに無い行を連番で足す。 */
    public OrderCsvFixture invalidRows(int count, String orderNumberPrefix) {
        for (int i = 1; i <= count; i++) {
            row("%s%04d".formatted(orderNumberPrefix, i), "UNKNOWN", "P001",
                    "1", "100.00", "2026-01-18 10:00:00");
        }
        return this;
    }

    public Path writeTo(Path directory, String fileName) {
        Path file = directory.resolve(fileName);
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("テスト用 CSV の書き出しに失敗しました: " + file, e);
        }
        return file;
    }
}
