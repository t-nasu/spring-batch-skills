package com.example.pointbatch.domain.model.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pointbatch.domain.exception.IllegalPointStateException;
import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberPointTest {

    private static final BusinessDate BUSINESS_DATE = BusinessDate.of(LocalDate.of(2026, 4, 1));

    private static MemberPoint point(LocalDate expiresOn, PointStatus status) {
        return MemberPoint.reconstruct(
                MemberPointId.of(10L), MemberId.of(100L), PointAmount.of(500), expiresOn, status);
    }

    @Nested
    @DisplayName("isExpiredAsOf")
    class IsExpiredAsOf {

        @Test
        @DisplayName("有効期限が業務日付より前なら期限切れ")
        void beforeBusinessDate() {
            assertThat(point(LocalDate.of(2026, 3, 31), PointStatus.ACTIVE).isExpiredAsOf(BUSINESS_DATE))
                    .isTrue();
        }

        @Test
        @DisplayName("有効期限が業務日付と同日ならまだ有効(境界値)")
        void sameAsBusinessDate() {
            assertThat(point(LocalDate.of(2026, 4, 1), PointStatus.ACTIVE).isExpiredAsOf(BUSINESS_DATE))
                    .isFalse();
        }

        @Test
        @DisplayName("有効期限が業務日付より後ならまだ有効")
        void afterBusinessDate() {
            assertThat(point(LocalDate.of(2026, 4, 2), PointStatus.ACTIVE).isExpiredAsOf(BUSINESS_DATE))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        @DisplayName("期限切れの有効ポイントを失効させ、失効履歴を生成する")
        void expiresAndCreatesHistory() {
            MemberPoint target = point(LocalDate.of(2026, 3, 31), PointStatus.ACTIVE);

            PointExpirationHistory history = target.expire(BUSINESS_DATE);

            assertThat(target.status()).isEqualTo(PointStatus.EXPIRED);
            assertThat(history.memberPointId()).isEqualTo(MemberPointId.of(10L));
            assertThat(history.memberId()).isEqualTo(MemberId.of(100L));
            assertThat(history.amount()).isEqualTo(PointAmount.of(500));
            // 失効日は業務日付
            assertThat(history.expiredOn()).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("有効状態でないポイントは失効できない")
        void rejectsNonActivePoint() {
            MemberPoint used = point(LocalDate.of(2026, 3, 31), PointStatus.USED);

            assertThatThrownBy(() -> used.expire(BUSINESS_DATE))
                    .isInstanceOf(IllegalPointStateException.class)
                    .hasMessageContaining("有効状態でない");
            assertThat(used.status()).isEqualTo(PointStatus.USED);
        }

        @Test
        @DisplayName("有効期限内のポイントは失効できない")
        void rejectsNotYetExpiredPoint() {
            MemberPoint active = point(LocalDate.of(2026, 4, 1), PointStatus.ACTIVE);

            assertThatThrownBy(() -> active.expire(BUSINESS_DATE))
                    .isInstanceOf(IllegalPointStateException.class)
                    .hasMessageContaining("有効期限内");
            assertThat(active.status()).isEqualTo(PointStatus.ACTIVE);
        }
    }

    @Test
    @DisplayName("有効期限が NULL のデータは不正データとして扱う")
    void rejectsNullExpiresOn() {
        assertThatThrownBy(() -> point(null, PointStatus.ACTIVE))
                .isInstanceOf(InvalidPointDataException.class)
                .hasMessageContaining("有効期限");
    }
}
