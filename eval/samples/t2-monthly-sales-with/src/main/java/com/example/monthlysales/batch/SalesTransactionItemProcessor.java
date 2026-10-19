package com.example.monthlysales.batch;

import com.example.monthlysales.application.SummarizeSalesTransactionUseCase;
import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/** 取引明細 1 件をユースケースへ渡し、その取引ぶんの月次売上集計に置き換える。 */
public class SalesTransactionItemProcessor
        implements ItemProcessor<SalesTransaction, MonthlySalesSummary> {

    private final SummarizeSalesTransactionUseCase useCase;
    private final TargetMonth targetMonth;

    public SalesTransactionItemProcessor(SummarizeSalesTransactionUseCase useCase,
            TargetMonth targetMonth) {
        this.useCase = useCase;
        this.targetMonth = targetMonth;
    }

    @Override
    public MonthlySalesSummary process(SalesTransaction item) {
        return useCase.summarize(targetMonth, item);
    }
}
