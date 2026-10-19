package com.example.salesbatch.infrastructure.repository;

import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.SalesTransactionCursor;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;
import com.example.salesbatch.infrastructure.mybatis.mapper.SalesTransactionMapper;
import com.example.salesbatch.infrastructure.mybatis.record.SalesTransactionRecord;

import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * {@link SalesTransactionRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisSalesTransactionRepository implements SalesTransactionRepository {

    private final SalesTransactionMapper salesTransactionMapper;

    public MyBatisSalesTransactionRepository(SalesTransactionMapper salesTransactionMapper) {
        this.salesTransactionMapper = salesTransactionMapper;
    }

    @Override
    public List<RawSalesTransaction> findPage(
            TargetMonth targetMonth, SalesTransactionCursor cursor, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit は 1 以上です: " + limit);
        }

        String afterStoreId = null;
        String afterProductId = null;
        Long afterTransactionId = null;
        switch (cursor) {
            case SalesTransactionCursor.Start ignored -> {
                // 先頭から読む
            }
            case SalesTransactionCursor.AfterGroup afterGroup -> {
                afterStoreId = afterGroup.groupKey().storeId().value();
                afterProductId = afterGroup.groupKey().productId().value();
            }
            case SalesTransactionCursor.AfterRow afterRow -> {
                afterStoreId = afterRow.storeId();
                afterProductId = afterRow.productId();
                afterTransactionId = afterRow.transactionId();
            }
        }

        List<SalesTransactionRecord> records = salesTransactionMapper.selectPage(
                targetMonth.firstDate(),
                targetMonth.firstDateOfNextMonth(),
                afterStoreId,
                afterProductId,
                afterTransactionId,
                limit);

        return records.stream().map(MyBatisSalesTransactionRepository::toDomain).toList();
    }

    private static RawSalesTransaction toDomain(SalesTransactionRecord record) {
        return new RawSalesTransaction(
                record.transactionId(),
                record.storeId(),
                record.productId(),
                record.transactionDate(),
                record.type(),
                record.amount(),
                record.quantity());
    }
}
