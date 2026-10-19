package com.example.salesbatch.application.service;

import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;

import java.util.Objects;

/**
 * 対象年月ごとに {@link MonthlySalesGroupLoader} を組み立てるファクトリ。
 *
 * <p>ローダーと読み飛ばしポリシーはステートフル（ステップ 1 回分の状態を持つ）なので、
 * 起動のたびに生成する。</p>
 */
public class MonthlySalesGroupLoaderFactory {

    private final SalesTransactionRepository salesTransactionRepository;
    private final int pageSize;
    private final int skipLimit;

    public MonthlySalesGroupLoaderFactory(
            SalesTransactionRepository salesTransactionRepository, int pageSize, int skipLimit) {
        this.salesTransactionRepository =
                Objects.requireNonNull(salesTransactionRepository, "salesTransactionRepository は必須です");
        this.pageSize = pageSize;
        this.skipLimit = skipLimit;
    }

    public MonthlySalesGroupLoader create(TargetMonth targetMonth) {
        return new MonthlySalesGroupLoader(
                salesTransactionRepository, new InvalidTransactionSkipPolicy(skipLimit), targetMonth, pageSize);
    }
}
