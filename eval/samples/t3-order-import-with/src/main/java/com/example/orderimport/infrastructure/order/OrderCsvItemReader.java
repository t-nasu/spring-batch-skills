package com.example.orderimport.infrastructure.order;

import com.example.orderimport.domain.customer.CustomerCode;
import com.example.orderimport.domain.order.InvalidOrderException;
import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.order.OrderNumber;
import com.example.orderimport.domain.order.Quantity;
import com.example.orderimport.domain.product.ProductCode;
import com.example.orderimport.domain.shared.Money;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 取引先から届く注文 CSV を 1 行ずつ読み、注文（ドメインオブジェクト）に変換する。
 *
 * <p>読み取る対象は起動引数 {@code inputFile} が指す 1 ファイルのみ。UTF-8、先頭にヘッダ行があり、
 * 列は「注文番号, 顧客コード, 商品コード, 数量, 単価, 注文日時」の順である（仕様 3）。
 *
 * <p>読み取り位置を実行コンテキストに保存するため、失敗しても次の実行は読み終えた行の続きから始まる（仕様 6）。
 */
@Component
@StepScope
public class OrderCsvItemReader implements ItemStreamReader<Order> {

    /** CSV の列。仕様 3 の並びに合わせる。 */
    static final String[] COLUMNS = {
            "orderNumber", "customerCode", "productCode", "quantity", "unitPrice", "orderedAt"
    };

    /** 注文日時の書式。 */
    static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FlatFileItemReader<Order> delegate;

    public OrderCsvItemReader(@Value("#{jobParameters['inputFile']}") String inputFile) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(COLUMNS);
        tokenizer.setStrict(true);

        this.delegate = new FlatFileItemReaderBuilder<Order>()
                .name("orderCsvItemReader")
                .resource(new FileSystemResource(inputFile))
                .encoding(StandardCharsets.UTF_8.name())
                .linesToSkip(1)
                .lineTokenizer(tokenizer)
                .fieldSetMapper(OrderCsvItemReader::toOrder)
                .saveState(true)
                .build();
    }

    /**
     * CSV の 1 行を注文に変換する。変換できない行は {@link InvalidOrderException} とし、
     * 上限の範囲で読み飛ばせるようにする。
     */
    static Order toOrder(FieldSet fieldSet) {
        try {
            return new Order(
                    OrderNumber.of(fieldSet.readString("orderNumber")),
                    CustomerCode.of(fieldSet.readString("customerCode")),
                    ProductCode.of(fieldSet.readString("productCode")),
                    Quantity.of(fieldSet.readInt("quantity")),
                    Money.of(fieldSet.readBigDecimal("unitPrice")),
                    LocalDateTime.parse(fieldSet.readString("orderedAt").trim(), ORDERED_AT_FORMAT));
        } catch (InvalidOrderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidOrderException("注文として読めない行である: " + e.getMessage(), e);
        }
    }

    @Override
    public Order read() throws Exception {
        return delegate.read();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
