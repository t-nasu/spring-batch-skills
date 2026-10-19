package com.example.orderimport.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.orderimport.config.OrderImportProperties;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.repository.CustomerRepository;
import com.example.orderimport.domain.repository.ProductRepository;
import com.example.orderimport.domain.service.OrderValidationService;
import com.example.orderimport.domain.validation.ValidationError;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 1 行の検証ロジックの単体テスト。
 *
 * <p>マスタは実在するコードの集合を持つだけの偽物に差し替え、DB なしで検証規則
 * そのものを確かめる。
 */
class OrderLineImportServiceTest {

    private static final BusinessDate BUSINESS_DATE = BusinessDate.parse("20260118");

    private final OrderLineImportService service = new OrderLineImportService(
            new OrderValidationService(
                    new FakeCustomerRepository(Set.of("C001", "C002")),
                    new FakeProductRepository(Set.of("P001", "P002"))),
            defaultProperties());

    private static OrderImportProperties defaultProperties() {
        return new OrderImportProperties(1000, 100, "UTF-8", "yyyy-MM-dd HH:mm:ss", Duration.ofHours(6));
    }

    private static ImportOrderCommand validCommand() {
        return new ImportOrderCommand(2, "ORD-0001", "C001", "P001", "3", "1980.50", "2026-01-18 10:30:00");
    }

    @Nested
    @DisplayName("検証を通る行")
    class Valid {

        @Test
        @DisplayName("注文に変換され、業務日付はジョブ引数の値になる")
        void 注文に変換される() {
            Order order = service.toOrder(validCommand(), BUSINESS_DATE);

            assertThat(order.orderNumber().value()).isEqualTo("ORD-0001");
            assertThat(order.customerCode()).isEqualTo(CustomerCode.of("C001"));
            assertThat(order.productCode()).isEqualTo(ProductCode.of("P001"));
            assertThat(order.quantity().value()).isEqualTo(3);
            assertThat(order.unitPrice()).isEqualTo(Money.parse("1980.50"));
            assertThat(order.orderedAt()).isEqualTo(LocalDateTime.of(2026, 1, 18, 10, 30, 0));
            assertThat(order.amount()).isEqualTo(Money.parse("5941.50"));

            // 注文日時が別日でも、帰属日はジョブ引数の業務日付で決まる。
            assertThat(order.businessDate()).isEqualTo(BUSINESS_DATE);
        }

        @Test
        @DisplayName("注文日時が業務日付と違っていても業務日付は上書きされない")
        void 注文日時と業務日付は独立している() {
            ImportOrderCommand command = new ImportOrderCommand(
                    2, "ORD-0002", "C001", "P001", "1", "100", "2026-01-17 23:59:59");

            Order order = service.toOrder(command, BUSINESS_DATE);

            assertThat(order.businessDate()).isEqualTo(BUSINESS_DATE);
            assertThat(order.orderedAt()).isEqualTo(LocalDateTime.of(2026, 1, 17, 23, 59, 59));
        }
    }

    @Nested
    @DisplayName("仕様で定められた検証")
    class SpecifiedRules {

        @Test
        @DisplayName("顧客コードがマスタに無ければ弾く")
        void 顧客が存在しない() {
            ImportOrderCommand command = new ImportOrderCommand(
                    5, "ORD-0003", "C999", "P001", "1", "100", "2026-01-18 10:00:00");

            assertThatThrownBy(() -> service.toOrder(command, BUSINESS_DATE))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("顧客マスタに存在しません");
        }

        @Test
        @DisplayName("商品コードがマスタに無ければ弾く")
        void 商品が存在しない() {
            ImportOrderCommand command = new ImportOrderCommand(
                    6, "ORD-0004", "C001", "P999", "1", "100", "2026-01-18 10:00:00");

            assertThatThrownBy(() -> service.toOrder(command, BUSINESS_DATE))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("商品マスタに存在しません");
        }

        @Test
        @DisplayName("数量が 1 未満なら弾く")
        void 数量が一未満() {
            ImportOrderCommand command = new ImportOrderCommand(
                    7, "ORD-0005", "C001", "P001", "0", "100", "2026-01-18 10:00:00");

            assertThatThrownBy(() -> service.toOrder(command, BUSINESS_DATE))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("数量は 1 以上");
        }
    }

    @Nested
    @DisplayName("エラーの報告のしかた")
    class ErrorReporting {

        @Test
        @DisplayName("1 行に複数の誤りがあれば、最初の 1 件で止めずに全部報告する")
        void 複数のエラーをまとめて報告する() {
            ImportOrderCommand command = new ImportOrderCommand(
                    9, "ORD-0006", "C001", "P001", "0", "とても高い", "きのう");

            OrderValidationException exception = catchThrowableOfType(
                    OrderValidationException.class, () -> service.toOrder(command, BUSINESS_DATE));

            assertThat(exception.errors())
                    .extracting(ValidationError::field)
                    .containsExactlyInAnyOrder("数量", "単価", "注文日時");
        }

        @Test
        @DisplayName("行番号と注文番号を添えて、どの行かが分かるようにする")
        void 行を特定できる() {
            ImportOrderCommand command = new ImportOrderCommand(
                    42, "ORD-0007", "C999", "P001", "1", "100", "2026-01-18 10:00:00");

            OrderValidationException exception = catchThrowableOfType(
                    OrderValidationException.class, () -> service.toOrder(command, BUSINESS_DATE));

            assertThat(exception.lineNumber()).isEqualTo(42);
            assertThat(exception.orderNumber()).isEqualTo("ORD-0007");
            assertThat(exception.getMessage()).contains("42 行目", "ORD-0007");
        }

        @Test
        @DisplayName("コードが読めていない行ではマスタ照会まで進まない")
        void 変換に失敗したらマスタ照会しない() {
            // 顧客コードが空なので、存在チェック以前の問題として報告されるべき。
            ImportOrderCommand command = new ImportOrderCommand(
                    11, "ORD-0008", "", "P001", "1", "100", "2026-01-18 10:00:00");

            OrderValidationException exception = catchThrowableOfType(
                    OrderValidationException.class, () -> service.toOrder(command, BUSINESS_DATE));

            assertThat(exception.errors()).hasSize(1);
            assertThat(exception.errors().get(0).field()).isEqualTo("顧客コード");
            assertThat(exception.errors().get(0).message()).contains("空です");
        }
    }

    private record FakeCustomerRepository(Set<String> existingCodes) implements CustomerRepository {
        @Override
        public boolean exists(CustomerCode customerCode) {
            return existingCodes.contains(customerCode.value());
        }
    }

    private record FakeProductRepository(Set<String> existingCodes) implements ProductRepository {
        @Override
        public boolean exists(ProductCode productCode) {
            return existingCodes.contains(productCode.value());
        }
    }
}
