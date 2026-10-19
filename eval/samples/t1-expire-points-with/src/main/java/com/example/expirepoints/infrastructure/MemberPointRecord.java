package com.example.expirepoints.infrastructure;

import java.time.LocalDate;

/**
 * {@code member_point} 表の一行。
 *
 * <p>列をそのまま写すだけの DTO であり、業務上の規則は持たない。ドメインへの変換は
 * {@link MyBatisMemberPointRepository} が行う。
 */
public class MemberPointRecord {

    private Long id;
    private Long memberId;
    private Integer points;
    private LocalDate expiresOn;
    private String status;

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

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
