package com.example.pointbatch.domain.model.point;

import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ポイント失効履歴。
 *
 * <p>仕様で要求される項目は「会員 ID・ポイント数・失効日」。
 * どの会員ポイントを失効させたかを追跡できるよう {@code memberPointId} も保持する。
 *
 * @param memberPointId 失効させた会員ポイントの ID
 * @param memberId      会員 ID
 * @param amount        失効したポイント数
 * @param expiredOn     失効日(失効処理を行った業務日付)
 */
public record PointExpirationHistory(
        MemberPointId memberPointId, MemberId memberId, PointAmount amount, LocalDate expiredOn) {

    public PointExpirationHistory {
        Objects.requireNonNull(memberPointId, "会員ポイント ID は必須です");
        Objects.requireNonNull(memberId, "会員 ID は必須です");
        Objects.requireNonNull(amount, "ポイント数は必須です");
        Objects.requireNonNull(expiredOn, "失効日は必須です");
    }

    static PointExpirationHistory record(
            MemberPointId memberPointId,
            MemberId memberId,
            PointAmount amount,
            BusinessDate businessDate) {
        return new PointExpirationHistory(memberPointId, memberId, amount, businessDate.value());
    }
}
