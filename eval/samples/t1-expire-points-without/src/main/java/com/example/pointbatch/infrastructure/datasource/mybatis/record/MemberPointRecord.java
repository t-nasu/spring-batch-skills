package com.example.pointbatch.infrastructure.datasource.mybatis.record;

import java.time.LocalDate;

/**
 * member_point テーブルの行に対応する DTO。
 *
 * <p>MyBatis の結果マッピング用。ドメインモデルへの変換はリポジトリ実装が行う。
 */
public class MemberPointRecord {

    private Long id;
    private Long memberId;
    private Integer amount;
    private String status;
    private LocalDate expiresOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }
}
