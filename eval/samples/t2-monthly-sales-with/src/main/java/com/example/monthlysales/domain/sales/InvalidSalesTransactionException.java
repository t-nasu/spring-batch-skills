package com.example.monthlysales.domain.sales;

/**
 * 取引明細が業務上の決まりを満たしていないことを表す。
 *
 * <p>この例外が投げられた取引は集計から飛ばし、取引 ID をログに残す。
 */
public class InvalidSalesTransactionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 取引 ID が読み取れなかったときに代わりに使う表示。 */
    private static final String UNKNOWN_ID = "(不明)";

    private final String transactionId;
    private final String reason;

    public InvalidSalesTransactionException(String transactionId, String reason) {
        super("取引 ID=" + displayOf(transactionId) + " は不正である: " + reason);
        this.transactionId = displayOf(transactionId);
        this.reason = reason;
    }

    private static String displayOf(String transactionId) {
        return transactionId == null || transactionId.isBlank() ? UNKNOWN_ID : transactionId;
    }

    /** 飛ばした取引の ID。読み取れなかった場合は {@code (不明)}。 */
    public String transactionId() {
        return transactionId;
    }

    /** 不正と判断した理由。 */
    public String reason() {
        return reason;
    }
}
