package com.example.orderimport.application.order;

import com.example.orderimport.domain.validation.ValidationError;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入力ファイルの 1 行が検証を通らなかったことを表す例外。
 *
 * <p>この例外はステップのスキップ対象として設定されている。送出されるとその行は
 * 登録されずに読み飛ばされ、スキップ件数が上限（既定 100 件）に達した時点で
 * ジョブ全体が異常終了する。
 */
public class OrderValidationException extends RuntimeException {

    private final transient int lineNumber;
    private final transient String orderNumber;
    private final transient List<ValidationError> errors;

    public OrderValidationException(int lineNumber, String orderNumber, List<ValidationError> errors) {
        super(buildMessage(lineNumber, orderNumber, errors));
        this.lineNumber = lineNumber;
        this.orderNumber = orderNumber;
        this.errors = List.copyOf(errors);
    }

    private static String buildMessage(int lineNumber, String orderNumber, List<ValidationError> errors) {
        String detail = errors.stream()
                .map(ValidationError::toString)
                .collect(Collectors.joining(" / "));
        return "%d 行目（注文番号=%s）の検証に失敗しました: %s"
                .formatted(lineNumber, orderNumber == null ? "(不明)" : orderNumber, detail);
    }

    public int lineNumber() {
        return lineNumber;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
