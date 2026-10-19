package com.example.salesbatch.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.Money;
import com.example.salesbatch.domain.model.Quantity;
import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.MonthlySalesSummaryRepository;
import com.example.salesbatch.domain.repository.SalesTransactionCursor;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * MyBatis のマッパー（SQL）の結合テスト。
 */
@SpringBootTest
@ActiveProfiles("test")
class MyBatisRepositoryIntegrationTest {

    private static final TargetMonth TARGET_MONTH = TargetMonth.of("202509");

    @Autowired
    private SalesTransactionRepository salesTransactionRepository;

    @Autowired
    private MonthlySalesSummaryRepository monthlySalesSummaryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sales_transaction");
        jdbcTemplate.execute("DELETE FROM monthly_sales_summary");

        insert(1L, "S001", "P001", "2025-09-01");
        insert(2L, "S001", "P001", "2025-09-02");
        insert(3L, "S001", "P002", "2025-09-03");
        insert(4L, "S002", "P001", "2025-09-04");
        insert(5L, "S001", "P001", "2025-08-31"); // 対象年月外
        insert(6L, "S001", "P001", "2025-10-01"); // 対象年月外
    }

    @Test
    @DisplayName("対象年月の明細を店舗・商品・取引 ID 順に取得する")
    void 先頭から取得する() {
        List<RawSalesTransaction> page =
                salesTransactionRepository.findPage(TARGET_MONTH, SalesTransactionCursor.start(), 10);

        assertThat(page).extracting(RawSalesTransaction::transactionId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(page.get(0).storeId()).isEqualTo("S001");
        assertThat(page.get(0).type()).isEqualTo("SALE");
        assertThat(page.get(0).amount()).isEqualByComparingTo("100.00");
        assertThat(page.get(0).quantity()).isEqualTo(1);
        assertThat(page.get(0).transactionDate()).isEqualTo(LocalDate.of(2025, 9, 1));
    }

    @Test
    @DisplayName("取得件数の上限が効く")
    void ページサイズで区切る() {
        List<RawSalesTransaction> page =
                salesTransactionRepository.findPage(TARGET_MONTH, SalesTransactionCursor.start(), 2);

        assertThat(page).extracting(RawSalesTransaction::transactionId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("行の位置を指定すると、その行より後ろを取得する")
    void 行の続きから取得する() {
        List<RawSalesTransaction> page = salesTransactionRepository.findPage(
                TARGET_MONTH, SalesTransactionCursor.afterRow("S001", "P001", 1L), 10);

        assertThat(page).extracting(RawSalesTransaction::transactionId).containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("集計単位の位置を指定すると、その店舗・商品の明細はすべて読み飛ばす")
    void 集計単位の続きから取得する() {
        List<RawSalesTransaction> page = salesTransactionRepository.findPage(
                TARGET_MONTH, SalesTransactionCursor.afterGroup(SalesGroupKey.of("S001", "P001")), 10);

        assertThat(page).extracting(RawSalesTransaction::transactionId).containsExactly(3L, 4L);
    }

    @Test
    @DisplayName("同じキーで登録し直すと上書きされる")
    void 登録は冪等である() {
        monthlySalesSummaryRepository.saveAll(List.of(summary("100.00", 1, 1)));
        monthlySalesSummaryRepository.saveAll(List.of(summary("250.50", 5, 7)));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT total_amount, total_quantity, transaction_count, created_at, updated_at "
                        + "FROM monthly_sales_summary");

        assertThat(rows).hasSize(1);
        assertThat((BigDecimal) rows.get(0).get("total_amount")).isEqualByComparingTo("250.50");
        assertThat(rows.get(0).get("total_quantity")).isEqualTo(5);
        assertThat(rows.get(0).get("transaction_count")).isEqualTo(7);
    }

    @Test
    @DisplayName("対象年月を指定して削除する")
    void 対象年月の集計結果を削除する() {
        monthlySalesSummaryRepository.saveAll(List.of(summary("100.00", 1, 1)));

        assertThat(monthlySalesSummaryRepository.deleteByTargetMonth(TargetMonth.of("202508"))).isZero();
        assertThat(monthlySalesSummaryRepository.deleteByTargetMonth(TARGET_MONTH)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM monthly_sales_summary", Integer.class))
                .isZero();
    }

    private static MonthlySalesSummary summary(String totalAmount, int totalQuantity, int transactionCount) {
        return new MonthlySalesSummary(
                TARGET_MONTH,
                SalesGroupKey.of("S001", "P001"),
                Money.of(new BigDecimal(totalAmount)),
                Quantity.of(totalQuantity),
                transactionCount);
    }

    private void insert(long transactionId, String storeId, String productId, String transactionDate) {
        jdbcTemplate.update("""
                INSERT INTO sales_transaction
                  (transaction_id, store_id, product_id, transaction_date, type, amount, quantity)
                VALUES (?, ?, ?, ?, 'SALE', 100.00, 1)
                """, transactionId, storeId, productId, LocalDate.parse(transactionDate));
    }
}
