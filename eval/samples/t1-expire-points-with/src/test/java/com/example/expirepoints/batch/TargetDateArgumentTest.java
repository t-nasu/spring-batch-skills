package com.example.expirepoints.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class TargetDateArgumentTest {

    @Test
    void targetDate_を業務日付として取り出す() {
        var args = new DefaultApplicationArguments("--targetDate=20261019");

        assertThat(TargetDateArgument.from(args).value()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void 指定が無ければ弾く() {
        var args = new DefaultApplicationArguments();

        assertThatThrownBy(() -> TargetDateArgument.from(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetDate");
    }

    @Test
    void 二つ以上指定されていれば弾く() {
        var args = new DefaultApplicationArguments("--targetDate=20261019", "--targetDate=20261020");

        assertThatThrownBy(() -> TargetDateArgument.from(args))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 形式が合わなければ弾く() {
        var args = new DefaultApplicationArguments("--targetDate=2026-10-19");

        assertThatThrownBy(() -> TargetDateArgument.from(args))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
