package com.example.expirepoints.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberPointTest {

    private static final BusinessDate TARGET_DATE = BusinessDate.parse("20261019");

    private static MemberPoint point(LocalDate expiresOn, PointStatus status) {
        return new MemberPoint(
                new MemberPointId(1L), new MemberId(100L), new PointAmount(50), expiresOn, status);
    }

    @Nested
    @DisplayName("失効対象かどうか")
    class IsExpirableAsOf {

        @Test
        void 有効期限が業務日付より前で_ACTIVE_なら対象である() {
            assertThat(point(LocalDate.of(2026, 10, 18), PointStatus.ACTIVE)
                            .isExpirableAsOf(TARGET_DATE))
                    .isTrue();
        }

        @Test
        void 有効期限が業務日付と同じ日なら対象でない() {
            assertThat(point(LocalDate.of(2026, 10, 19), PointStatus.ACTIVE)
                            .isExpirableAsOf(TARGET_DATE))
                    .isFalse();
        }

        @Test
        void 有効期限が先ならば対象でない() {
            assertThat(point(LocalDate.of(2026, 10, 20), PointStatus.ACTIVE)
                            .isExpirableAsOf(TARGET_DATE))
                    .isFalse();
        }

        @Test
        void すでに失効していれば対象でない() {
            assertThat(point(LocalDate.of(2026, 10, 18), PointStatus.EXPIRED)
                            .isExpirableAsOf(TARGET_DATE))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("失効させる")
    class Expire {

        @Test
        void 状態だけが_EXPIRED_になった写しを返す() {
            MemberPoint active = point(LocalDate.of(2026, 10, 18), PointStatus.ACTIVE);

            MemberPoint expired = active.expire(TARGET_DATE);

            assertThat(expired.status()).isEqualTo(PointStatus.EXPIRED);
            assertThat(expired.isExpired()).isTrue();
            assertThat(expired.id()).isEqualTo(active.id());
            assertThat(expired.memberId()).isEqualTo(active.memberId());
            assertThat(expired.amount()).isEqualTo(active.amount());
            assertThat(expired.expiresOn()).isEqualTo(active.expiresOn());
            assertThat(active.status())
                    .as("元のインスタンスは変わらない")
                    .isEqualTo(PointStatus.ACTIVE);
        }

        @Test
        void 対象でないポイントは失効させられない() {
            MemberPoint notYet = point(LocalDate.of(2026, 10, 20), PointStatus.ACTIVE);

            assertThatThrownBy(() -> notYet.expire(TARGET_DATE))
                    .isInstanceOf(PointNotExpirableException.class);
        }
    }

    @Nested
    @DisplayName("不正データ")
    class InvalidData {

        @Test
        void 有効期限が空なら作れない() {
            assertThatThrownBy(() -> point(null, PointStatus.ACTIVE))
                    .isInstanceOf(InvalidMemberPointException.class);
        }

        @Test
        void ポイント数が_0_以下なら作れない() {
            assertThatThrownBy(() -> new PointAmount(0))
                    .isInstanceOf(InvalidMemberPointException.class);
            assertThatThrownBy(() -> PointAmount.of(null))
                    .isInstanceOf(InvalidMemberPointException.class);
        }

        @Test
        void 会員_ID_が_0_以下なら作れない() {
            assertThatThrownBy(() -> new MemberId(0L))
                    .isInstanceOf(InvalidMemberPointException.class);
            assertThatThrownBy(() -> MemberId.of(null))
                    .isInstanceOf(InvalidMemberPointException.class);
        }

        @Test
        void 会員ポイント_ID_が_0_以下なら作れない() {
            assertThatThrownBy(() -> new MemberPointId(-1L))
                    .isInstanceOf(InvalidMemberPointException.class);
            assertThatThrownBy(() -> MemberPointId.of(null))
                    .isInstanceOf(InvalidMemberPointException.class);
        }

        @Test
        void 知らない状態なら作れない() {
            assertThatThrownBy(() -> PointStatus.of("USED"))
                    .isInstanceOf(InvalidMemberPointException.class);
            assertThatThrownBy(() -> PointStatus.of(null))
                    .isInstanceOf(InvalidMemberPointException.class);
        }
    }

    @Nested
    @DisplayName("失効履歴")
    class History {

        @Test
        void 失効させたポイントから作る() {
            MemberPoint expired =
                    point(LocalDate.of(2026, 10, 18), PointStatus.ACTIVE).expire(TARGET_DATE);

            PointExpirationHistory history = PointExpirationHistory.of(expired, TARGET_DATE);

            assertThat(history.memberPointId()).isEqualTo(expired.id());
            assertThat(history.memberId()).isEqualTo(expired.memberId());
            assertThat(history.amount()).isEqualTo(expired.amount());
            assertThat(history.expiredOn()).isEqualTo(LocalDate.of(2026, 10, 19));
        }

        @Test
        void 失効していないポイントからは作れない() {
            MemberPoint active = point(LocalDate.of(2026, 10, 18), PointStatus.ACTIVE);

            assertThatThrownBy(() -> PointExpirationHistory.of(active, TARGET_DATE))
                    .isInstanceOf(PointNotExpirableException.class);
        }
    }
}
