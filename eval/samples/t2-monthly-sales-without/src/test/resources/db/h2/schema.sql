-- テスト用（H2 の MySQL 互換モード）。本番の DDL は src/main/resources/db/mysql/schema.sql。

DROP TABLE IF EXISTS sales_transaction;
CREATE TABLE sales_transaction (
  transaction_id   BIGINT        NOT NULL,
  store_id         VARCHAR(10)   NOT NULL,
  product_id       VARCHAR(20)   NOT NULL,
  transaction_date DATE          NOT NULL,
  type             VARCHAR(10)   NOT NULL,
  amount           DECIMAL(13,2),
  quantity         INT,
  PRIMARY KEY (transaction_id)
);
CREATE INDEX idx_sales_transaction_group
  ON sales_transaction (store_id, product_id, transaction_id);

DROP TABLE IF EXISTS monthly_sales_summary;
CREATE TABLE monthly_sales_summary (
  target_month      CHAR(6)       NOT NULL,
  store_id          VARCHAR(10)   NOT NULL,
  product_id        VARCHAR(20)   NOT NULL,
  total_amount      DECIMAL(15,2) NOT NULL,
  total_quantity    INT           NOT NULL,
  transaction_count INT           NOT NULL,
  created_at        TIMESTAMP(3)  NOT NULL,
  updated_at        TIMESTAMP(3)  NOT NULL,
  PRIMARY KEY (target_month, store_id, product_id)
);
