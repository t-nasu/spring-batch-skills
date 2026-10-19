package com.example.pointbatch.domain.model.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectTest {

    @Test
    @DisplayName("PointAmount: 0 は許容し、負値は不正データ")
    void pointAmount() {
        assertThat(PointAmount.of(0).value()).isZero();
        assertThat(PointAmount.of(100).value()).isEqualTo(100);
        assertThatThrownBy(() -> PointAmount.of(-1)).isInstanceOf(InvalidPointDataException.class);
    }

    @Test
    @DisplayName("MemberId / MemberPointId: 0 以下は不正データ")
    void identifiers() {
        assertThat(MemberId.of(1L).value()).isEqualTo(1L);
        assertThatThrownBy(() -> MemberId.of(0L)).isInstanceOf(InvalidPointDataException.class);
        assertThatThrownBy(() -> MemberPointId.of(-5L)).isInstanceOf(InvalidPointDataException.class);
    }

    @Test
    @DisplayName("PointStatus: 未知の状態値は不正データ")
    void pointStatus() {
        assertThat(PointStatus.of("ACTIVE")).isEqualTo(PointStatus.ACTIVE);
        assertThatThrownBy(() -> PointStatus.of("UNKNOWN"))
                .isInstanceOf(InvalidPointDataException.class);
        assertThatThrownBy(() -> PointStatus.of(null)).isInstanceOf(InvalidPointDataException.class);
    }

    @Test
    @DisplayName("BusinessDate: yyyyMMdd 形式を解釈する")
    void businessDateParse() {
        assertThat(BusinessDate.parse("20260401").value()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(BusinessDate.of(LocalDate.of(2026, 4, 1))).hasToString("20260401");
    }

    @ParameterizedTest
    @DisplayName("BusinessDate: 形式不正・存在しない日付は不正データ")
    @ValueSource(strings = {"2026-04-01", "202604", "20260230", "20261301", "abcdefgh", ""})
    void businessDateParseFailure(String value) {
        assertThatThrownBy(() -> BusinessDate.parse(value))
                .isInstanceOf(InvalidPointDataException.class);
    }
}
