package com.example.monthlysales.application;

import com.example.monthlysales.domain.sales.InvalidSalesTransactionException;
import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import org.springframework.stereotype.Component;

/** 取引明細 1 件を、その取引だけを計上した月次売上集計に置き換える。 */
@Component
public class SummarizeSalesTransactionUseCase {

    /**
     * @throws InvalidSalesTransactionException 取引日が対象年月の外にあるとき
     */
    public MonthlySalesSummary summarize(TargetMonth targetMonth, SalesTransaction transaction) {
        if (!targetMonth.contains(transaction.transactionDate())) {
            throw new InvalidSalesTransactionException(transaction.id().value(),
                    "取引日 " + transaction.transactionDate()
                            + " が対象年月 " + targetMonth.format() + " の外にある");
        }
        return MonthlySalesSummary.of(targetMonth, transaction);
    }
}
