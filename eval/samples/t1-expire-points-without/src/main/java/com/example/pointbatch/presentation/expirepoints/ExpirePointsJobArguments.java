package com.example.pointbatch.presentation.expirepoints;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.util.Arrays;

/**
 * expirePointsJob の起動引数。
 *
 * <p>形式: {@code targetDate=yyyyMMdd}(Spring Boot 流の {@code --targetDate=yyyyMMdd} も受け付ける)
 *
 * @param targetDate 業務日付
 */
public record ExpirePointsJobArguments(BusinessDate targetDate) {

    private static final String KEY = "targetDate";
    private static final String USAGE = "使用方法: java -jar point-batch.jar targetDate=yyyyMMdd";

    /**
     * コマンドライン引数を解析する。
     *
     * @throws JobArgumentException 引数が無い・重複している・形式が不正な場合
     */
    public static ExpirePointsJobArguments parse(String... args) {
        String[] safeArgs = args == null ? new String[0] : args;

        var values =
                Arrays.stream(safeArgs)
                        .map(ExpirePointsJobArguments::stripOptionPrefix)
                        .filter(arg -> arg.startsWith(KEY + "="))
                        .map(arg -> arg.substring((KEY + "=").length()))
                        .toList();

        if (values.isEmpty()) {
            throw new JobArgumentException("起動引数 %s が指定されていません。%s".formatted(KEY, USAGE));
        }
        if (values.size() > 1) {
            throw new JobArgumentException("起動引数 %s が複数指定されています: %s。%s".formatted(KEY, values, USAGE));
        }

        try {
            return new ExpirePointsJobArguments(BusinessDate.parse(values.get(0)));
        } catch (InvalidPointDataException e) {
            throw new JobArgumentException("%s。%s".formatted(e.getMessage(), USAGE), e);
        }
    }

    private static String stripOptionPrefix(String arg) {
        return arg.startsWith("--") ? arg.substring(2) : arg;
    }
}
