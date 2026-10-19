package com.example.expirepoints.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 会員が保有するポイントの一口。
 *
 * <p>有効期限（{@code expiresOn}）を業務日付が過ぎており、かつ状態が {@link PointStatus#ACTIVE}
 * のものが失効の対象になる。有効期限当日はまだ使えるものとし、失効させない。
 */
public final class MemberPoint {

    private final MemberPointId id;
    private final MemberId memberId;
    private final PointAmount amount;
    private final LocalDate expiresOn;
    private final PointStatus status;

    public MemberPoint(
            MemberPointId id,
            MemberId memberId,
            PointAmount amount,
            LocalDate expiresOn,
            PointStatus status) {
        this.id = Objects.requireNonNull(id, "会員ポイント ID は必須である");
        this.memberId = Objects.requireNonNull(memberId, "会員 ID は必須である");
        this.amount = Objects.requireNonNull(amount, "ポイント数は必須である");
        if (expiresOn == null) {
            throw new InvalidMemberPointException("有効期限が空である: id=" + id.value());
        }
        this.expiresOn = expiresOn;
        this.status = Objects.requireNonNull(status, "ポイント状態は必須である");
    }

    /** 業務日付の時点で失効させるべきか。 */
    public boolean isExpirableAsOf(BusinessDate businessDate) {
        return status == PointStatus.ACTIVE && expiresOn.isBefore(businessDate.value());
    }

    /**
     * このポイントを失効させた姿を返す。元のインスタンスは変わらない。
     *
     * @throws PointNotExpirableException 業務日付の時点で失効対象でないとき
     */
    public MemberPoint expire(BusinessDate businessDate) {
        if (!isExpirableAsOf(businessDate)) {
            throw new PointNotExpirableException(
                    "失効対象でないポイントを失効させようとした: id=%d, status=%s, expiresOn=%s, businessDate=%s"
                            .formatted(id.value(), status, expiresOn, businessDate));
        }
        return new MemberPoint(id, memberId, amount, expiresOn, PointStatus.EXPIRED);
    }

    public boolean isExpired() {
        return status == PointStatus.EXPIRED;
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
        return o instanceof MemberPoint other
                && id.equals(other.id)
                && memberId.equals(other.memberId)
                && amount.equals(other.amount)
                && expiresOn.equals(other.expiresOn)
                && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, memberId, amount, expiresOn, status);
    }

    @Override
    public String toString() {
        return "MemberPoint[id=%d, memberId=%d, amount=%d, expiresOn=%s, status=%s]"
                .formatted(id.value(), memberId.value(), amount.value(), expiresOn, status);
    }
}
