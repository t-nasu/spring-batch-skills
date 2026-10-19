package com.example.expirepoints;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.expirepoints.batch.ExpirePointsRunner;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 組み込み DB を相手に expirePointsJob を通しで動かし、Mapper XML の SQL まで含めて確かめる。
 *
 * <p>データは {@code src/test/resources/schema.sql} と {@code data.sql} が、DataSource の初期化時
 * （= {@link ExpirePointsRunner} が走る前）に流し込む。
 */
@SpringBootTest(
        args = "--targetDate=20261019",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:expirepoints;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            // 初期化スクリプトを確定させるため、ここだけ自動コミットにする。
            "spring.datasource.hikari.auto-commit=true",
            "spring.sql.init.mode=always"
        })
class ExpirePointsBatchIntegrationTest {

    @Autowired private ExpirePointsRunner runner;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void 期限切れのポイントだけが失効し_履歴が残る() {
        assertThat(runner.getExitCode()).as("正常終了している").isZero();

        assertThat(statusById())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                1L, "EXPIRED",
                                2L, "EXPIRED",
                                3L, "ACTIVE",
                                4L, "ACTIVE",
                                5L, "EXPIRED"));

        List<HistoryRow> histories =
                jdbcTemplate.query(
                        "SELECT member_point_id, member_id, points, expired_on"
                                + " FROM point_expiration_history ORDER BY member_point_id",
                        (rs, rowNum) ->
                                new HistoryRow(
                                        rs.getLong("member_point_id"),
                                        rs.getLong("member_id"),
                                        rs.getInt("points"),
                                        rs.getObject("expired_on", LocalDate.class)));

        LocalDate expiredOn = LocalDate.of(2026, 10, 19);
        assertThat(histories)
                .containsExactly(
                        new HistoryRow(1L, 100L, 50, expiredOn),
                        new HistoryRow(2L, 100L, 30, expiredOn));
    }

    private record HistoryRow(long memberPointId, long memberId, int points, LocalDate expiredOn) {}

    private Map<Long, String> statusById() {
        return jdbcTemplate.query(
                "SELECT id, status FROM member_point",
                rs -> {
                    Map<Long, String> statuses = new java.util.HashMap<>();
                    while (rs.next()) {
                        statuses.put(rs.getLong("id"), rs.getString("status"));
                    }
                    return statuses;
                });
    }
}
