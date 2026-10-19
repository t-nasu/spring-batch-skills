package com.example.salesbatch.presentation.batch.writer;

import com.example.salesbatch.application.service.MonthlySalesSummaryRegistrationService;
import com.example.salesbatch.domain.model.MonthlySalesSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 集計結果を月次売上表へ登録する {@code ItemWriter}。
 */
@Component
public class MonthlySalesSummaryItemWriter implements ItemWriter<MonthlySalesSummary> {

    private final MonthlySalesSummaryRegistrationService registrationService;

    public MonthlySalesSummaryItemWriter(MonthlySalesSummaryRegistrationService registrationService) {
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService は必須です");
    }

    @Override
    public void write(Chunk<? extends MonthlySalesSummary> chunk) {
        List<MonthlySalesSummary> summaries = new ArrayList<>(chunk.getItems());
        registrationService.register(summaries);
    }
}
