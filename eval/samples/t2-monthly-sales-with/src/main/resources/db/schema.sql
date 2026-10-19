-- 月次売上集計バッチが読み書きする表。
-- Spring Batch の管理表（BATCH_JOB_INSTANCE ほか）は spring-batch-core の
-- org/springframework/batch/core/schema-mysql.sql を使って別途作成する。

-- 入力: 取引明細。
-- 列は NOT NULL だが、区分コードの誤りや負の金額といった壊れた値は入りうるため、
-- 読み出したあとドメイン側で検査し、不正なものは飛ばす。
CREATE TABLE IF NOT EXISTS sales_transaction (
    transaction_id   VARCHAR(32)    NOT NULL,
    store_id         VARCHAR(16)    NOT NULL,
    product_id       VARCHAR(32)    NOT NULL,
    transaction_date DATE           NOT NULL,
    type             VARCHAR(16)    NOT NULL COMMENT 'SALE または RETURN',
    amount           DECIMAL(15, 2) NOT NULL,
    quantity         INT            NOT NULL,
    PRIMARY KEY (transaction_id),
    -- 対象年月での絞り込みと読み出し順を、並べ替え無しで賄うための索引
    KEY idx_sales_transaction_date (transaction_date, transaction_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 出力: 店舗別・商品別の月次売上集計。
-- 主キーに対する加算登録（INSERT ... ON DUPLICATE KEY UPDATE）で積み上げる。
CREATE TABLE IF NOT EXISTS monthly_sales_summary (
    target_month   CHAR(6)        NOT NULL COMMENT 'yyyyMM',
    store_id       VARCHAR(16)    NOT NULL,
    product_id     VARCHAR(32)    NOT NULL,
    total_amount   DECIMAL(18, 2) NOT NULL,
    total_quantity BIGINT         NOT NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (target_month, store_id, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
