-- orderImportJob が使う業務表（MySQL 8.0.19 以降を想定）。
-- Spring Batch のメタデータ表は、Spring Batch の配布物に入っている
-- org/springframework/batch/core/schema-mysql.sql を使って別に作る。

CREATE TABLE IF NOT EXISTS customers (
    customer_code VARCHAR(32)  NOT NULL,
    customer_name VARCHAR(128) NOT NULL,
    PRIMARY KEY (customer_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    product_code VARCHAR(32)  NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    PRIMARY KEY (product_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 取り込んだ注文。注文番号で一意に決まるため、再開時に同じ行を読んでも重複しない。
CREATE TABLE IF NOT EXISTS orders (
    order_number  VARCHAR(32)   NOT NULL,
    customer_code VARCHAR(32)   NOT NULL,
    product_code  VARCHAR(32)   NOT NULL,
    quantity      INT           NOT NULL,
    unit_price    DECIMAL(13, 2) NOT NULL,
    amount        DECIMAL(15, 2) NOT NULL,
    ordered_at    DATETIME      NOT NULL,
    order_date    DATE          NOT NULL,
    PRIMARY KEY (order_number),
    KEY idx_orders_order_date (order_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 業務日付ごとの日次売上。
CREATE TABLE IF NOT EXISTS daily_sales (
    business_date DATE           NOT NULL,
    order_count   BIGINT         NOT NULL,
    total_amount  DECIMAL(18, 2) NOT NULL,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (business_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
