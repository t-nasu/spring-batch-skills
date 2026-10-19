package com.example.orderimport.infrastructure;

import com.example.orderimport.infrastructure.order.OrderRecord;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper の XML が読めること、Mapper インタフェースの各メソッドに対応する SQL があることを確かめる。
 * DB には接続しない。
 */
class MapperXmlTest {

    private static final List<String> MAPPER_XMLS = List.of(
            "mybatis/mapper/OrderMapper.xml",
            "mybatis/mapper/CustomerMapper.xml",
            "mybatis/mapper/ProductMapper.xml",
            "mybatis/mapper/DailySalesMapper.xml");

    private Configuration configuration;

    @BeforeEach
    void parseMapperXmls() throws Exception {
        configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        for (String resource : MAPPER_XMLS) {
            try (InputStream in = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @Test
    @DisplayName("Mapper インタフェースの各メソッドに対応する SQL がある")
    void everyMapperMethodHasStatement() {
        assertThat(List.of(
                "com.example.orderimport.infrastructure.order.OrderMapper.upsertAll",
                "com.example.orderimport.infrastructure.order.OrderMapper.selectSalesSummary",
                "com.example.orderimport.infrastructure.customer.CustomerMapper.countByCustomerCode",
                "com.example.orderimport.infrastructure.product.ProductMapper.countByProductCode",
                "com.example.orderimport.infrastructure.sales.DailySalesMapper.upsert"))
                .allSatisfy(id -> assertThat(configuration.hasStatement(id)).as(id).isTrue());
    }

    @Test
    @DisplayName("注文の一括登録は件数ぶんの行と上書き指定を組み立てる")
    void buildsBulkUpsertSql() {
        MappedStatement statement = configuration.getMappedStatement(
                "com.example.orderimport.infrastructure.order.OrderMapper.upsertAll");
        Map<String, Object> parameter = Map.of("orders", List.of(orderRecord("ORD-001"), orderRecord("ORD-002")));

        BoundSql boundSql = statement.getBoundSql(parameter);

        assertThat(boundSql.getSql()).contains("INSERT INTO orders", "ON DUPLICATE KEY UPDATE");
        // 2 件ぶんの値（1 件あたり 8 列）が並ぶ
        assertThat(boundSql.getParameterMappings()).hasSize(16);
    }

    @Test
    @DisplayName("集計の結果は集計用の DTO に対応づける")
    void salesSummaryMapsToRecord() {
        MappedStatement statement = configuration.getMappedStatement(
                "com.example.orderimport.infrastructure.order.OrderMapper.selectSalesSummary");

        assertThat(statement.getResultMaps().getFirst().getType())
                .isEqualTo(com.example.orderimport.infrastructure.order.OrderSalesSummaryRecord.class);
    }

    private static OrderRecord orderRecord(String orderNumber) {
        OrderRecord record = new OrderRecord();
        record.setOrderNumber(orderNumber);
        record.setCustomerCode("C001");
        record.setProductCode("P001");
        record.setQuantity(1);
        record.setUnitPrice(new BigDecimal("100.00"));
        record.setAmount(new BigDecimal("100.00"));
        record.setOrderedAt(LocalDateTime.of(2026, 10, 18, 9, 0));
        record.setOrderDate(LocalDate.of(2026, 10, 18));
        return record;
    }
}
