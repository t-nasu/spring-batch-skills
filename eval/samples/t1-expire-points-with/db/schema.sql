-- expirePointsJob が読み書きする表。
-- このファイルはアプリから自動実行しない。環境構築時に手で流す。

CREATE TABLE IF NOT EXISTS member_point (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    points     INT         NOT NULL,
    expires_on DATE        NOT NULL,
    status     VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_member_point_expiry (status, expires_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS point_expiration_history (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    member_point_id BIGINT NOT NULL,
    member_id       BIGINT NOT NULL,
    points          INT    NOT NULL,
    expired_on      DATE   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_point_expiration_history_point (member_point_id),
    KEY idx_point_expiration_history_member (member_id, expired_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
