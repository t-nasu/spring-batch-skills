package com.example.orderimport.domain.order;

import com.example.orderimport.domain.customer.CustomerCode;
import com.example.orderimport.domain.customer.CustomerRepository;
import com.example.orderimport.domain.product.ProductCode;
import com.example.orderimport.domain.product.ProductRepository;
import com.example.orderimport.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderValidatorTest {

    private final CustomerRepository customers = code -> Set.of("C001").contains(code.value());
    private final ProductRepository products = code -> Set.of("P001").contains(code.value());
    private final OrderValidator validator = new OrderValidator(customers, products);

    @Test
    @DisplayName("顧客と商品がどちらも存在すれば通る")
    void acceptsOrderWhenCustomerAndProductExist() {
        assertThatCode(() -> validator.validate(order("C001", "P001"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("顧客コードが存在しなければ弾く")
    void rejectsUnknownCustomer() {
        assertThatThrownBy(() -> validator.validate(order("C999", "P001")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("顧客コードが存在しない");
    }

    @Test
    @DisplayName("商品コードが存在しなければ弾く")
    void rejectsUnknownProduct() {
        assertThatThrownBy(() -> validator.validate(order("C001", "P999")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("商品コードが存在しない");
    }

    private static Order order(String customerCode, String productCode) {
        return new Order(
                OrderNumber.of("ORD-001"),
                CustomerCode.of(customerCode),
                ProductCode.of(productCode),
                Quantity.of(1),
                Money.of(new BigDecimal("100")),
                LocalDateTime.of(2026, 10, 18, 10, 0, 0));
    }
}
