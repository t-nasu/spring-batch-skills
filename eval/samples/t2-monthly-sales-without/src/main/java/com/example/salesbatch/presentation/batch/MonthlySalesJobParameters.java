package com.example.salesbatch.presentation.batch;

/**
 * 月次売上集計ジョブの起動引数の名前。
 */
public final class MonthlySalesJobParameters {

    /**
     * 対象年月（{@code yyyyMM}）。
     *
     * <p>ジョブインスタンスを識別するパラメータなので、同じ対象年月で正常終了したジョブを
     * もう一度起動することはできない（Spring Batch が {@code JobInstanceAlreadyCompleteException}
     * で拒否する）。これにより同一対象年月の二重起動を防ぐ。</p>
     */
    public static final String TARGET_MONTH = "targetMonth";

    private MonthlySalesJobParameters() {
    }
}
