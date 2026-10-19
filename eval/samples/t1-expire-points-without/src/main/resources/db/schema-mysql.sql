-- expirePointsJob が利用するテーブル定義 (MySQL 8.0)

CREATE TABLE IF NOT EXISTS member_point (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会員ポイント ID',
    member_id   BIGINT       NOT NULL                COMMENT '会員 ID',
    amount      INT          NOT NULL                COMMENT 'ポイント数',
    status      VARCHAR(16)  NOT NULL                COMMENT '状態 (ACTIVE / USED / EXPIRED)',
    expires_on  DATE         NOT NULL                COMMENT '有効期限 (この日までは有効)',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 失効対象の抽出 (status = 'ACTIVE' AND expires_on < :targetDate ORDER BY id) 用
    KEY idx_member_point_status_expires_on (status, expires_on, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会員ポイント';

CREATE TABLE IF NOT EXISTS point_expiration_history (
    id              BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ポイント失効履歴 ID',
    member_point_id BIGINT   NOT NULL                COMMENT '失効させた会員ポイント ID',
    member_id       BIGINT   NOT NULL                COMMENT '会員 ID',
    amount          INT      NOT NULL                COMMENT '失効したポイント数',
    expired_on      DATE     NOT NULL                COMMENT '失効日 (失効処理を行った業務日付)',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 同じ会員ポイントの失効履歴が二重登録されないことを DB レベルで保証する
    UNIQUE KEY uk_point_expiration_history_member_point_id (member_point_id),
    KEY idx_point_expiration_history_member_id (member_id, expired_on)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ポイント失効履歴';
