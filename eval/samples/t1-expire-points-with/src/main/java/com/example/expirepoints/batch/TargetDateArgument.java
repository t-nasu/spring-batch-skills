package com.example.expirepoints.batch;

import com.example.expirepoints.domain.BusinessDate;
import java.util.List;
import org.springframework.boot.ApplicationArguments;

/** 起動引数 {@code --targetDate=yyyyMMdd} の解釈。 */
final class TargetDateArgument {

    static final String NAME = "targetDate";

    private TargetDateArgument() {}

    /**
     * 起動引数から業務日付を取り出す。
     *
     * @throws IllegalArgumentException 指定が無い、二つ以上ある、形式が合わないとき
     */
    static BusinessDate from(ApplicationArguments args) {
        if (!args.containsOption(NAME)) {
            throw new IllegalArgumentException(
                    "起動引数 --%s=yyyyMMdd が指定されていない".formatted(NAME));
        }
        List<String> values = args.getOptionValues(NAME);
        if (values == null || values.size() != 1) {
            throw new IllegalArgumentException(
                    "起動引数 --%s は一つだけ指定する: %s".formatted(NAME, values));
        }
        return BusinessDate.parse(values.getFirst());
    }
}
