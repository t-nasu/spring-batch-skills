package com.example.monthlysales.infrastructure.summary;

import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 月次売上集計を monthly_sales_summary 表へ加算登録する。 */
@Repository
public class MyBatisMonthlySalesSummaryRepository implements MonthlySalesSummaryRepository {

    private final MonthlySalesSummaryMapper mapper;

    public MyBatisMonthlySalesSummaryRepository(MonthlySalesSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void addAll(List<MonthlySalesSummary> summaries) {
        if (summaries.isEmpty()) {
            return;
        }
        mapper.addAll(summaries.stream().map(MyBatisMonthlySalesSummaryRepository::toRow).toList());
    }

    private static MonthlySalesSummaryRow toRow(MonthlySalesSummary summary) {
        return new MonthlySalesSummaryRow(
                summary.targetMonth().format(),
                summary.storeId().value(),
                summary.productId().value(),
                summary.totalAmount().value(),
                summary.totalQuantity().value());
    }
}
