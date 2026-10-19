package com.example.salesbatch.application.exception;

/**
 * 読み飛ばした不正データが上限を超えたことを表す例外。これが送出されるとジョブは異常終了する。
 */
public class SkipLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int limit;
    private final int skippedCount;

    public SkipLimitExceededException(int limit, int skippedCount, Throwable cause) {
        super("不正データの読み飛ばしが上限を超えました: limit=%d skipped=%d".formatted(limit, skippedCount), cause);
        this.limit = limit;
        this.skippedCount = skippedCount;
    }

    public int limit() {
        return limit;
    }

    public int skippedCount() {
        return skippedCount;
    }
}
