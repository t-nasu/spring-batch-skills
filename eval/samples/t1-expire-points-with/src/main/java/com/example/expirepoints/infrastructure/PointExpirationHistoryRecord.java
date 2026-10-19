package com.example.expirepoints.infrastructure;

import java.time.LocalDate;

/** {@code point_expiration_history} 表の一行。 */
public class PointExpirationHistoryRecord {

    private Long memberPointId;
    private Long memberId;
    private Integer points;
    private LocalDate expiredOn;

    public Long getMemberPointId() {
        return memberPointId;
    }

    public void setMemberPointId(Long memberPointId) {
        this.memberPointId = memberPointId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDate getExpiredOn() {
        return expiredOn;
    }

    public void setExpiredOn(LocalDate expiredOn) {
        this.expiredOn = expiredOn;
    }
}
