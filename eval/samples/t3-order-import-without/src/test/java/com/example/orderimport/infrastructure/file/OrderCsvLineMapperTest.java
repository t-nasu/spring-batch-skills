package com.example.orderimport.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderimport.application.order.ImportOrderCommand;
import com.example.orderimport.support.OrderCsvFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderCsvLineMapperTest {

    private final OrderCsvLineMapper mapper = new OrderCsvLineMapper();

    @Test
    @DisplayName("列を順番どおりに写し、解釈はしない")
    void 文字列のまま写す() throws Exception {
        ImportOrderCommand command =
                mapper.mapLine("ORD-0001,C001,P001,3,1980.50,2026-01-18 10:30:00", 2);

        assertThat(command.lineNumber()).isEqualTo(2);
        assertThat(command.orderNumber()).isEqualTo("ORD-0001");
        assertThat(command.customerCode()).isEqualTo("C001");
        assertThat(command.productCode()).isEqualTo("P001");
        assertThat(command.quantity()).isEqualTo("3");
        assertThat(command.unitPrice()).isEqualTo("1980.50");
        assertThat(command.orderedAt()).isEqualTo("2026-01-18 10:30:00");
    }

    @Test
    @DisplayName("不正な値もそのまま持ち帰る（判断は後続の検証に委ねる）")
    void 不正な値でも例外にしない() throws Exception {
        ImportOrderCommand command = mapper.mapLine("ORD-0002,C001,P001,ゼロ個,たかい,きのう", 3);

        assertThat(command.quantity()).isEqualTo("ゼロ個");
        assertThat(command.unitPrice()).isEqualTo("たかい");
        assertThat(command.orderedAt()).isEqualTo("きのう");
    }

    @Test
    @DisplayName("列数が足りない行は例外にする")
    void 列数不足を拒否する() {
        assertThatThrownBy(() -> mapper.mapLine("ORD-0003,C001,P001", 4))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("列数が多い行は例外にする")
    void 列数過多を拒否する() {
        assertThatThrownBy(() ->
                mapper.mapLine("ORD-0004,C001,P001,1,100,2026-01-18 10:00:00,余分", 5))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("想定どおりのヘッダ行なら通す")
    void 正しいヘッダを受け入れる() {
        OrderCsvHeaderValidator validator = new OrderCsvHeaderValidator();

        validator.handleLine(OrderCsvFixture.HEADER);
        // BOM 付きのファイルでも先頭列が化けないこと。
        validator.handleLine('﻿' + OrderCsvFixture.HEADER);
    }

    @Test
    @DisplayName("列が入れ替わったヘッダは、取り込む前に弾く")
    void 列順の入れ替わりを検知する() {
        OrderCsvHeaderValidator validator = new OrderCsvHeaderValidator();

        assertThatThrownBy(() ->
                validator.handleLine("注文番号,商品コード,顧客コード,数量,単価,注文日時"))
                .isInstanceOf(OrderCsvHeaderValidator.InvalidCsvHeaderException.class)
                .hasMessageContaining("ヘッダ行が想定と異なります");
    }
}
