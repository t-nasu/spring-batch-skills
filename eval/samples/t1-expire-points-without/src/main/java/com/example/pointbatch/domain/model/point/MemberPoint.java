package com.example.pointbatch.domain.model.point;

import com.example.pointbatch.domain.exception.IllegalPointStateException;
import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 会員ポイントを表す集約ルート。
 *
 * <p>「有効期限を過ぎたポイントを失効させる」という業務ルールはこのクラスが持つ。
 */
public class MemberPoint {

    private final MemberPointId id;
    private final MemberId memberId;
    private final PointAmount amount;
    private final LocalDate expiresOn;
    private PointStatus status;

    private MemberPoint(
            MemberPointId id,
            MemberId memberId,
            PointAmount amount,
            LocalDate expiresOn,
            PointStatus status) {
        this.id = Objects.requireNonNull(id, "会員ポイント ID は必須です");
        this.memberId = Objects.requireNonNull(memberId, "会員 ID は必須です");
        this.amount = Objects.requireNonNull(amount, "ポイント数は必須です");
        this.status = Objects.requireNonNull(status, "ポイント状態は必須です");
        if (expiresOn == null) {
            throw new InvalidPointDataException(
                    "有効期限が設定されていません: memberPointId=" + id);
        }
        this.expiresOn = expiresOn;
    }

    /**
     * 永続化済みの状態から復元する。
     */
    public static MemberPoint reconstruct(
            MemberPointId id,
            MemberId memberId,
            PointAmount amount,
            LocalDate expiresOn,
            PointStatus status) {
        return new MemberPoint(id, memberId, amount, expiresOn, status);
    }

    /**
     * 業務日付の時点で有効期限切れかどうかを判定する。
     *
     * <p>判定条件は仕様の抽出条件 {@code expires_on < targetDate} と一致させる。
     * すなわち有効期限当日はまだ有効であり、翌日以降に失効対象となる。
     */
    public boolean isExpiredAsOf(BusinessDate businessDate) {
        return expiresOn.isBefore(businessDate.value());
    }

    /**
     * 失効させられる状態かどうかを判定する。
     */
    public boolean canExpire(BusinessDate businessDate) {
        return status == PointStatus.ACTIVE && isExpiredAsOf(businessDate);
    }

    /**
     * ポイントを失効させ、失効履歴を生成する。
     *
     * @param businessDate 業務日付。これを失効日として履歴に記録する。
     * @return 生成された失効履歴
     * @throws IllegalPointStateException 有効状態でない、または有効期限内の場合
     */
    public PointExpirationHistory expire(BusinessDate businessDate) {
        Objects.requireNonNull(businessDate, "業務日付は必須です");
        if (status != PointStatus.ACTIVE) {
            throw new IllegalPointStateException(
                    "有効状態でないポイントは失効できません: memberPointId=%s, status=%s"
                            .formatted(id, status));
        }
        if (!isExpiredAsOf(businessDate)) {
            throw new IllegalPointStateException(
                    "有効期限内のポイントは失効できません: memberPointId=%s, expiresOn=%s, businessDate=%s"
                            .formatted(id, expiresOn, businessDate));
        }
        this.status = PointStatus.EXPIRED;
        return PointExpirationHistory.record(id, memberId, amount, businessDate);
    }

    public MemberPointId id() {
        return id;
    }

    public MemberId memberId() {
        return memberId;
    }

    public PointAmount amount() {
        return amount;
    }

    public LocalDate expiresOn() {
        return expiresOn;
    }

    public PointStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberPoint other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MemberPoint{id=%s, memberId=%s, amount=%s, expiresOn=%s, status=%s}"
                .formatted(id, memberId, amount, expiresOn, status);
    }
}
