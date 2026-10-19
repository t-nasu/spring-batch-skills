package com.example.salesbatch.testsupport;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.MonthlySalesSummaryRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * テスト用に、指定した回数目の登録で失敗させられる {@link MonthlySalesSummaryRepository}。
 * 「途中で失敗したジョブを再開する」ことを確かめるために使う。
 */
public class FailableMonthlySalesSummaryRepository implements MonthlySalesSummaryRepository {

    private final MonthlySalesSummaryRepository delegate;
    private final AtomicInteger saveCount = new AtomicInteger();

    private volatile int failOnSaveCall = -1;
    private volatile RuntimeException failureToThrow;

    public FailableMonthlySalesSummaryRepository(MonthlySalesSummaryRepository delegate) {
        this.delegate = delegate;
    }

    /**
     * 指定した回数目（1 始まり）の {@link #saveAll(List)} で例外を発生させる。
     */
    public void failOnSaveCall(int saveCall, RuntimeException failure) {
        this.failOnSaveCall = saveCall;
        this.failureToThrow = failure;
    }

    /** 失敗の設定と呼び出し回数を元に戻す。 */
    public void reset() {
        this.failOnSaveCall = -1;
        this.failureToThrow = null;
        this.saveCount.set(0);
    }

    public int saveCount() {
        return saveCount.get();
    }

    @Override
    public void saveAll(List<MonthlySalesSummary> summaries) {
        if (saveCount.incrementAndGet() == failOnSaveCall) {
            throw failureToThrow;
        }
        delegate.saveAll(summaries);
    }

    @Override
    public int deleteByTargetMonth(TargetMonth targetMonth) {
        return delegate.deleteByTargetMonth(targetMonth);
    }
}
