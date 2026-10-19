package com.example.salesbatch.infrastructure.repository;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.MonthlySalesSummaryRepository;
import com.example.salesbatch.infrastructure.mybatis.mapper.MonthlySalesSummaryMapper;
import com.example.salesbatch.infrastructure.mybatis.record.MonthlySalesSummaryRecord;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * {@link MonthlySalesSummaryRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisMonthlySalesSummaryRepository implements MonthlySalesSummaryRepository {

    private final MonthlySalesSummaryMapper monthlySalesSummaryMapper;
    private final Clock clock;

    public MyBatisMonthlySalesSummaryRepository(
            MonthlySalesSummaryMapper monthlySalesSummaryMapper, Clock clock) {
        this.monthlySalesSummaryMapper = monthlySalesSummaryMapper;
        this.clock = clock;
    }

    @Override
    public void saveAll(List<MonthlySalesSummary> summaries) {
        if (summaries.isEmpty()) {
            return;
        }
        List<MonthlySalesSummaryRecord> records = summaries.stream()
                .map(MyBatisMonthlySalesSummaryRepository::toRecord)
                .toList();
        monthlySalesSummaryMapper.upsertAll(records, LocalDateTime.now(clock));
    }

    @Override
    public int deleteByTargetMonth(TargetMonth targetMonth) {
        return monthlySalesSummaryMapper.deleteByTargetMonth(targetMonth.asString());
    }

    private static MonthlySalesSummaryRecord toRecord(MonthlySalesSummary summary) {
        return new MonthlySalesSummaryRecord(
                summary.targetMonth().asString(),
                summary.storeId().value(),
                summary.productId().value(),
                summary.totalAmount().value(),
                summary.totalQuantity().value(),
                summary.transactionCount());
    }
}
