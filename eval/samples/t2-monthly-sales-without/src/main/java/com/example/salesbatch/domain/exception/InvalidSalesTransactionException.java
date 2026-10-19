package com.example.salesbatch.domain.exception;

/**
 * 取引明細が集計に使えない（不正データである）ことを表す例外。
 *
 * <p>仕様上、不正データは処理を止めずに読み飛ばすため、この例外は上限件数の判定と
 * 取引 ID のログ出力のために送出される。</p>
 */
public class InvalidSalesTransactionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long transactionId;
    private final String reason;

    public InvalidSalesTransactionException(long transactionId, String reason) {
        this(transactionId, reason, null);
    }

    public InvalidSalesTransactionException(long transactionId, String reason, Throwable cause) {
        super("取引 ID=%d の取引明細が不正です: %s".formatted(transactionId, reason), cause);
        this.transactionId = transactionId;
        this.reason = reason;
    }

    /** 読み飛ばした取引 ID。ログ出力に使う。 */
    public long transactionId() {
        return transactionId;
    }

    /** 不正と判断した理由。 */
    public String reason() {
        return reason;
    }
}
