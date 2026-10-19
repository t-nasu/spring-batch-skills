package com.example.orderimport.application.order;

import com.example.orderimport.config.OrderImportProperties;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.model.order.OrderNumber;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.model.order.Quantity;
import com.example.orderimport.domain.service.OrderValidationService;
import com.example.orderimport.domain.validation.ValidationError;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * 入力ファイルの 1 行を検証して注文に組み立てるユースケース。
 *
 * <p>検証は 2 段階ある。
 * <ol>
 *   <li>各項目を値オブジェクトに変換する。書式不正や「数量が 1 未満」はここで弾かれる</li>
 *   <li>顧客・商品がマスタに実在するかをドメインサービスに問い合わせる</li>
 * </ol>
 *
 * <p>1 行の中に複数の誤りがあっても運用チームが一度に修正できるよう、最初のエラーで
 * 打ち切らずすべて収集してから {@link OrderValidationException} にまとめて載せる。
 * マスタ照会は値の変換が全件成功した場合にのみ行う（コードが読めていない状態で
 * 問い合わせても意味がないため）。
 */
@Service
public class OrderLineImportService {

    private final OrderValidationService orderValidationService;
    private final DateTimeFormatter orderedAtFormatter;

    public OrderLineImportService(OrderValidationService orderValidationService,
                                  OrderImportProperties properties) {
        this.orderValidationService = orderValidationService;
        this.orderedAtFormatter = DateTimeFormatter.ofPattern(properties.orderedAtFormat());
    }

    /**
     * 1 行を注文に変換する。
     *
     * @param command      解析前の 1 行
     * @param businessDate ジョブ引数で指定された業務日付
     * @return 検証を通過した注文
     * @throws OrderValidationException 1 件以上の検証エラーがある場合
     */
    public Order toOrder(ImportOrderCommand command, BusinessDate businessDate) {
        List<ValidationError> errors = new ArrayList<>();

        OrderNumber orderNumber = convert("注文番号", command.orderNumber(), OrderNumber::of, errors);
        CustomerCode customerCode = convert("顧客コード", command.customerCode(), CustomerCode::of, errors);
        ProductCode productCode = convert("商品コード", command.productCode(), ProductCode::of, errors);
        Quantity quantity = convert("数量", command.quantity(), Quantity::parse, errors);
        Money unitPrice = convert("単価", command.unitPrice(), Money::parse, errors);
        LocalDateTime orderedAt = convert("注文日時", command.orderedAt(), this::parseOrderedAt, errors);

        if (errors.isEmpty()) {
            errors.addAll(orderValidationService.validateMasterReferences(customerCode, productCode));
        }
        if (!errors.isEmpty()) {
            throw new OrderValidationException(command.lineNumber(), command.orderNumber(), errors);
        }

        return new Order(orderNumber, businessDate, customerCode, productCode,
                quantity, unitPrice, orderedAt);
    }

    /**
     * 1 項目を変換する。失敗しても例外を投げず {@code errors} に積んで null を返すため、
     * 呼び出し側は全項目の変換を続行できる。
     */
    private static <T> T convert(String field, String raw, Function<String, T> converter,
                                 List<ValidationError> errors) {
        try {
            return converter.apply(raw);
        } catch (IllegalArgumentException e) {
            errors.add(ValidationError.of(field, e.getMessage()));
            return null;
        }
    }

    private LocalDateTime parseOrderedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("注文日時が空です");
        }
        try {
            return LocalDateTime.parse(raw.trim(), orderedAtFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("注文日時の書式が不正です: " + raw, e);
        }
    }
}
