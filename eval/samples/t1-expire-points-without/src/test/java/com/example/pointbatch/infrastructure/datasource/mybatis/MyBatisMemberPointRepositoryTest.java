package com.example.pointbatch.infrastructure.datasource.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.exception.PointUpdateConflictException;
import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.point.MemberPoint;
import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.point.PointAmount;
import com.example.pointbatch.domain.model.point.PointStatus;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import com.example.pointbatch.domain.repository.MemberPointRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/schema-h2.sql")
class MyBatisMemberPointRepositoryTest {

    private static final BusinessDate TARGET_DATE = BusinessDate.of(LocalDate.of(2026, 4, 1));

    @Autowired private MemberPointRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private void insertPoint(long id, long memberId, int amount, String status, String expiresOn) {
        jdbcTemplate.update(
                "INSERT INTO member_point (id, member_id, amount, status, expires_on) VALUES (?, ?, ?, ?, ?)",
                id,
                memberId,
                amount,
                status,
                LocalDate.parse(expiresOn));
    }

    @Test
    @DisplayName("抽出条件(status = 'ACTIVE' かつ expires_on < targetDate)で ID 昇順に取得する")
    void findExpirationTargets() {
        insertPoint(1L, 100L, 10, "ACTIVE", "2026-03-31");
        insertPoint(2L, 100L, 20, "ACTIVE", "2026-04-01");
        insertPoint(3L, 100L, 30, "USED", "2026-03-01");
        insertPoint(4L, 100L, 40, "ACTIVE", "2026-02-01");

        List<MemberPoint> targets =
                repository.findExpirationTargets(TARGET_DATE, Optional.empty(), 10);

        assertThat(targets).extracting(point -> point.id().value()).containsExactly(1L, 4L);
        assertThat(targets.get(0).memberId()).isEqualTo(MemberId.of(100L));
        assertThat(targets.get(0).amount()).isEqualTo(PointAmount.of(10));
        assertThat(targets.get(0).status()).isEqualTo(PointStatus.ACTIVE);
        assertThat(targets.get(0).expiresOn()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("キーセットページングで指定 ID より後ろだけを limit 件まで取得する")
    void findExpirationTargetsWithCursorAndLimit() {
        for (long id = 1L; id <= 5L; id++) {
            insertPoint(id, 100L, 10, "ACTIVE", "2026-03-01");
        }

        List<MemberPoint> targets =
                repository.findExpirationTargets(TARGET_DATE, Optional.of(MemberPointId.of(2L)), 2);

        assertThat(targets).extracting(point -> point.id().value()).containsExactly(3L, 4L);
    }

    @Test
    @DisplayName("失効させたポイントの状態を更新する")
    void saveExpired() {
        insertPoint(1L, 100L, 10, "ACTIVE", "2026-03-01");
        insertPoint(2L, 100L, 20, "ACTIVE", "2026-03-01");

        List<MemberPoint> targets =
                repository.findExpirationTargets(TARGET_DATE, Optional.empty(), 10);
        targets.forEach(point -> point.expire(TARGET_DATE));
        repository.saveExpired(targets);

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM member_point WHERE status = 'EXPIRED'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("読み取り後に他処理が状態を変更していた場合は競合として失敗する")
    void saveExpiredDetectsConflict() {
        insertPoint(1L, 100L, 10, "ACTIVE", "2026-03-01");
        insertPoint(2L, 100L, 20, "ACTIVE", "2026-03-01");

        List<MemberPoint> targets =
                repository.findExpirationTargets(TARGET_DATE, Optional.empty(), 10);
        targets.forEach(point -> point.expire(TARGET_DATE));

        // 他処理が 1 件を使用済みにしたことを模擬する
        jdbcTemplate.update("UPDATE member_point SET status = 'USED' WHERE id = 2");

        assertThatThrownBy(() -> repository.saveExpired(targets))
                .isInstanceOf(PointUpdateConflictException.class)
                .hasMessageContaining("更新件数が一致しません");
    }

    @Test
    @DisplayName("不正データ(負のポイント数)を読み取った場合は例外を送出してジョブを止める")
    void rejectsInvalidData() {
        insertPoint(1L, 100L, -5, "ACTIVE", "2026-03-01");

        assertThatThrownBy(() -> repository.findExpirationTargets(TARGET_DATE, Optional.empty(), 10))
                .isInstanceOf(InvalidPointDataException.class)
                .hasMessageContaining("ポイント数が不正です");
    }
}
