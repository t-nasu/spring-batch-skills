-- テスト用スキーマ (H2 / MODE=MySQL)。
-- 本番の定義は src/main/resources/db/schema-mysql.sql。内容を変更したら両方を更新すること。

DROP TABLE IF EXISTS point_expiration_history;
DROP TABLE IF EXISTS member_point;

CREATE TABLE member_point (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    amount      INT         NOT NULL,
    status      VARCHAR(16) NOT NULL,
    expires_on  DATE        NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_member_point_status_expires_on ON member_point (status, expires_on, id);

CREATE TABLE point_expiration_history (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    member_point_id BIGINT   NOT NULL,
    member_id       BIGINT   NOT NULL,
    amount          INT      NOT NULL,
    expired_on      DATE     NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_point_expiration_history_member_point_id UNIQUE (member_point_id)
);

CREATE INDEX idx_point_expiration_history_member_id ON point_expiration_history (member_id, expired_on);
