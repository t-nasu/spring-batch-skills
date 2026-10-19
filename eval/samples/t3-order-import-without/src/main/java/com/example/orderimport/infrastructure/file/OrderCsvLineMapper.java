package com.example.orderimport.infrastructure.file;

import com.example.orderimport.application.order.ImportOrderCommand;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.file.transform.LineTokenizer;

/**
 * CSV の 1 行を {@link ImportOrderCommand} に写す。
 *
 * <p>型変換も検証も行わず、文字列のまま詰め替えるだけにしている。ここで変換まで
 * 行うと、変換失敗が「読み取りエラー」として扱われ、検証エラーと別の経路で
 * スキップされてしまう。不正データの扱いを一本化するため、解釈は後続の
 * {@link com.example.orderimport.application.order.OrderLineImportService} に任せる。
 *
 * <p>列数が合わない行だけは、ここに到達する前にトークナイザが例外を送出する。
 * これも不正データとしてスキップ対象に含めてある。
 */
public class OrderCsvLineMapper implements LineMapper<ImportOrderCommand> {

    private final LineTokenizer tokenizer;

    public OrderCsvLineMapper() {
        DelimitedLineTokenizer delimited = new DelimitedLineTokenizer(OrderCsvFormat.DELIMITER);
        delimited.setNames(OrderCsvFormat.FIELD_NAMES);
        // 列数が想定と違う行は黙って詰めずに例外にする。
        delimited.setStrict(true);
        this.tokenizer = delimited;
    }

    @Override
    public ImportOrderCommand mapLine(String line, int lineNumber) {
        FieldSet fieldSet = tokenizer.tokenize(line);
        return new ImportOrderCommand(
                lineNumber,
                fieldSet.readString("orderNumber"),
                fieldSet.readString("customerCode"),
                fieldSet.readString("productCode"),
                fieldSet.readString("quantity"),
                fieldSet.readString("unitPrice"),
                fieldSet.readString("orderedAt"));
    }
}
