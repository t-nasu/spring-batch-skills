package com.example.expirepoints.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * ポイントを失効させた記録。
 *
 * <p>失効日には、失効を行った業務日付を入れる。
 */
public record PointExpirationHistory(
        MemberPointId memberPointId, MemberId memberId, PointAmount amount, LocalDate expiredOn) {

    public PointExpirationHistory {
        Objects.requireNonNull(memberPointId, "会員ポイント ID は必須である");
        Objects.requireNonNull(memberId, "会員 ID は必須である");
        Objects.requireNonNull(amount, "ポイント数は必須である");
        Objects.requireNonNull(expiredOn, "失効日は必須である");
    }

    /** 失効させたポイントから履歴を作る。 */
    public static PointExpirationHistory of(MemberPoint expiredPoint, BusinessDate businessDate) {
        if (!expiredPoint.isExpired()) {
            throw new PointNotExpirableException(
                    "失効していないポイントの履歴は作れない: id=" + expiredPoint.id().value());
        }
        return new PointExpirationHistory(
                expiredPoint.id(),
                expiredPoint.memberId(),
                expiredPoint.amount(),
                businessDate.value());
    }
}
