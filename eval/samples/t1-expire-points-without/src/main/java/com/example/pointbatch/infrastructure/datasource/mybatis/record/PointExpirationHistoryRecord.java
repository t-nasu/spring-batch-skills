package com.example.pointbatch.infrastructure.datasource.mybatis.record;

import java.time.LocalDate;

/**
 * point_expiration_history テーブルの行に対応する DTO(登録用)。
 */
public class PointExpirationHistoryRecord {

    private Long memberPointId;
    private Long memberId;
    private Integer amount;
    private LocalDate expiredOn;

    public PointExpirationHistoryRecord() {
        // MyBatis 用
    }

    public PointExpirationHistoryRecord(
            Long memberPointId, Long memberId, Integer amount, LocalDate expiredOn) {
        this.memberPointId = memberPointId;
        this.memberId = memberId;
        this.amount = amount;
        this.expiredOn = expiredOn;
    }

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

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public LocalDate getExpiredOn() {
        return expiredOn;
    }

    public void setExpiredOn(LocalDate expiredOn) {
        this.expiredOn = expiredOn;
    }
}
