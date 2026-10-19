package com.example.orderimport.presentation.runner;

import com.example.orderimport.presentation.job.JobParameterNames;
import java.util.List;
import org.springframework.boot.ApplicationArguments;

/**
 * コマンドラインから受け取ったジョブ引数。
 *
 * <p>値の妥当性（日付の書式、ファイルの存在）はここでは見ない。ジョブ引数の検証は
 * {@link com.example.orderimport.presentation.job.OrderImportJobParametersValidator}
 * に一本化してあり、起動経路によって判定が食い違わないようにしている。
 *
 * @param targetDate 業務日付。未指定なら null
 * @param inputFile  入力ファイルのパス。未指定なら null
 */
public record JobArguments(String targetDate, String inputFile) {

    /**
     * {@code --targetDate=20260101} 形式と {@code targetDate=20260101} 形式の
     * どちらでも受け取る。
     *
     * <p>前者は Spring Boot の標準的なオプション引数、後者は Spring Batch の
     * コマンドラインランナーに合わせた書き方で、ジョブスケジューラの設定を
     * そのまま流用できるようにしている。
     */
    public static JobArguments parse(ApplicationArguments args) {
        return new JobArguments(
                valueOf(args, JobParameterNames.TARGET_DATE),
                valueOf(args, JobParameterNames.INPUT_FILE));
    }

    private static String valueOf(ApplicationArguments args, String name) {
        List<String> optionValues = args.getOptionValues(name);
        if (optionValues != null && !optionValues.isEmpty()) {
            return lastOf(optionValues);
        }
        String prefix = name + "=";
        List<String> nonOptionValues = args.getNonOptionArgs().stream()
                .filter(arg -> arg.startsWith(prefix))
                .map(arg -> arg.substring(prefix.length()))
                .toList();
        return nonOptionValues.isEmpty() ? null : lastOf(nonOptionValues);
    }

    /** 同じ引数が複数回指定された場合は、後に書かれたほうを採用する。 */
    private static String lastOf(List<String> values) {
        return values.get(values.size() - 1);
    }

    /** 運用チーム向けの起動方法の説明。 */
    public static String usage() {
        return """
                使い方: java -jar order-import-batch.jar \\
                          --%s=yyyyMMdd --%s=/path/to/orders_yyyyMMdd.csv

                  --%s  業務日付。取り込んだ注文の帰属日であり、日次売上の集計対象日
                  --%s  取り込む CSV ファイルのパス
                """.formatted(
                JobParameterNames.TARGET_DATE, JobParameterNames.INPUT_FILE,
                JobParameterNames.TARGET_DATE, JobParameterNames.INPUT_FILE);
    }
}
