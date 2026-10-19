package com.example.pointbatch.application.expirepoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.pointbatch.domain.model.shared.BusinessDate;

/**
 * expirePointsJob のユースケースを実 DB(H2 / MySQL 互換モード)に対して検証する。
 */
@SpringBootTest(properties = "batch.expire-points.chunk-size=3")
@ActiveProfiles("test")
@Sql(scripts = "classpath:db/schema-h2.sql")
class ExpirePointsIntegrationTest {

    private static final BusinessDate TARGET_DATE = BusinessDate.of(LocalDate.of(2026, 4, 1));

    @Autowired private ExpirePointsUseCase expirePointsUseCase;
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

    private String statusOf(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM member_point WHERE id = ?", String.class, id);
    }

    /** 検証用に失効履歴を読み出す。 */
    private record HistoryRow(long memberPointId, long memberId, int amount, LocalDate expiredOn) {}

    private List<HistoryRow> histories() {
        return jdbcTemplate.query(
                "SELECT member_point_id, member_id, amount, expired_on"
                        + " FROM point_expiration_history ORDER BY member_point_id",
                (rs, rowNum) ->
                        new HistoryRow(
                                rs.getLong("member_point_id"),
                                rs.getLong("member_id"),
                                rs.getInt("amount"),
                                rs.getObject("expired_on", LocalDate.class)));
    }

    @Test
    @DisplayName("失効対象だけを失効させ、失効履歴を登録する")
    void expiresOnlyTargets() {
        insertPoint(1L, 100L, 500, "ACTIVE", "2026-03-31"); // 対象: 期限切れかつ有効
        insertPoint(2L, 100L, 300, "ACTIVE", "2026-04-01"); // 対象外: 有効期限当日はまだ有効
        insertPoint(3L, 200L, 700, "ACTIVE", "2026-01-15"); // 対象
        insertPoint(4L, 200L, 100, "USED", "2026-03-01"); // 対象外: 使用済み
        insertPoint(5L, 300L, 200, "EXPIRED", "2026-03-01"); // 対象外: 失効済み
        insertPoint(6L, 300L, 900, "ACTIVE", "2026-12-31"); // 対象外: 期限内

        ExpirePointsResult result = expirePointsUseCase.execute(TARGET_DATE);

        assertThat(result.expiredPointCount()).isEqualTo(2L);
        assertThat(result.expiredAmountTotal()).isEqualTo(1200L);

        assertThat(statusOf(1L)).isEqualTo("EXPIRED");
        assertThat(statusOf(3L)).isEqualTo("EXPIRED");
        assertThat(statusOf(2L)).isEqualTo("ACTIVE");
        assertThat(statusOf(4L)).isEqualTo("USED");
        assertThat(statusOf(5L)).isEqualTo("EXPIRED");
        assertThat(statusOf(6L)).isEqualTo("ACTIVE");

        assertThat(histories())
                .containsExactly(
                        new HistoryRow(1L, 100L, 500, LocalDate.of(2026, 4, 1)),
                        new HistoryRow(3L, 200L, 700, LocalDate.of(2026, 4, 1)));
    }

    @Test
    @DisplayName("チャンクサイズを超える件数も分割して全件処理する")
    void processesAcrossChunks() {
        for (long id = 1L; id <= 7L; id++) {
            insertPoint(id, 100L + id, 10, "ACTIVE", "2026-03-01");
        }

        ExpirePointsResult result = expirePointsUseCase.execute(TARGET_DATE);

        // chunk-size=3 なので 3 + 3 + 1 の 3 コミット
        assertThat(result.expiredPointCount()).isEqualTo(7L);
        assertThat(result.expiredAmountTotal()).isEqualTo(70L);
        assertThat(result.chunkCount()).isEqualTo(3);

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM member_point WHERE status = 'ACTIVE'", Integer.class))
                .isZero();
        assertThat(histories()).hasSize(7);
    }

    @Test
    @DisplayName("再実行しても二重に失効・履歴登録されない")
    void isIdempotentOnRerun() {
        insertPoint(1L, 100L, 500, "ACTIVE", "2026-03-31");
        insertPoint(2L, 200L, 700, "ACTIVE", "2026-01-15");

        ExpirePointsResult first = expirePointsUseCase.execute(TARGET_DATE);
        ExpirePointsResult second = expirePointsUseCase.execute(TARGET_DATE);

        assertThat(first.expiredPointCount()).isEqualTo(2L);
        assertThat(second.expiredPointCount()).isZero();
        assertThat(second.chunkCount()).isZero();
        assertThat(histories()).hasSize(2);
    }

    @Test
    @DisplayName("対象が 1 件も無い場合も正常終了する")
    void noTargets() {
        insertPoint(1L, 100L, 500, "ACTIVE", "2026-12-31");

        ExpirePointsResult result = expirePointsUseCase.execute(TARGET_DATE);

        assertThat(result).isEqualTo(ExpirePointsResult.empty());
        assertThat(statusOf(1L)).isEqualTo("ACTIVE");
        assertThat(histories()).isEmpty();
    }
}
