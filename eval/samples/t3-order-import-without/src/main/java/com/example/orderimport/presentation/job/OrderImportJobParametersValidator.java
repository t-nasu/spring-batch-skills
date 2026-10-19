package com.example.orderimport.presentation.job;

import com.example.orderimport.domain.model.BusinessDate;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.stereotype.Component;

/**
 * ジョブ引数の検証。
 *
 * <p>30 万件の読み込みに入ってから引数の誤りで落ちると、再実行までの時間が無駄に
 * なるうえ中途半端なデータが残る。引数の不備はジョブ開始前にすべて洗い出す。
 */
@Component
public class OrderImportJobParametersValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        List<String> errors = new ArrayList<>();

        validateTargetDate(parameters.getString(JobParameterNames.TARGET_DATE), errors);
        validateInputFile(parameters.getString(JobParameterNames.INPUT_FILE), errors);

        if (!errors.isEmpty()) {
            throw new InvalidJobParametersException("ジョブ引数が不正です: " + String.join(" / ", errors));
        }
    }

    private void validateTargetDate(String targetDate, List<String> errors) {
        if (targetDate == null || targetDate.isBlank()) {
            errors.add("%s は必須です".formatted(JobParameterNames.TARGET_DATE));
            return;
        }
        try {
            BusinessDate.parse(targetDate);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
    }

    private void validateInputFile(String inputFile, List<String> errors) {
        if (inputFile == null || inputFile.isBlank()) {
            errors.add("%s は必須です".formatted(JobParameterNames.INPUT_FILE));
            return;
        }
        Path path;
        try {
            path = Path.of(inputFile);
        } catch (InvalidPathException e) {
            errors.add("%s がパスとして解釈できません: %s".formatted(JobParameterNames.INPUT_FILE, inputFile));
            return;
        }
        if (!Files.exists(path)) {
            errors.add("入力ファイルが存在しません: " + inputFile);
        } else if (!Files.isReadable(path)) {
            errors.add("入力ファイルを読み取れません: " + inputFile);
        }
    }
}
