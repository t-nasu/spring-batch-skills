-- 月次売上集計バッチが使う表（MySQL 8.x）。
--
-- Spring Batch のメタデータ表（BATCH_*）は別途
-- classpath:org/springframework/batch/core/schema-mysql.sql を流して作成すること。

-- ---------------------------------------------------------------------------
-- 入力: 取引明細
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales_transaction (
  transaction_id   BIGINT        NOT NULL COMMENT '取引 ID',
  store_id         VARCHAR(10)   NOT NULL COMMENT '店舗 ID',
  product_id       VARCHAR(20)   NOT NULL COMMENT '商品 ID',
  transaction_date DATE          NOT NULL COMMENT '取引日',
  type             VARCHAR(10)   NOT NULL COMMENT '取引区分 (SALE / RETURN)',
  amount           DECIMAL(13,2)          COMMENT '金額',
  quantity         INT                    COMMENT '数量',
  PRIMARY KEY (transaction_id),

  -- バッチは (店舗 ID, 商品 ID, 取引 ID) の昇順で読む。
  -- 並べ替え（filesort）を避け、かつ参照する列をすべて索引に含めて
  -- 表本体へのランダムアクセスを無くすためのカバリング索引。
  -- 索引サイズを抑えたい場合は末尾 4 列を外してもよい（表本体の参照が発生する）。
  KEY idx_sales_transaction_group (
    store_id, product_id, transaction_id, transaction_date, type, amount, quantity
  )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '取引明細';

-- ---------------------------------------------------------------------------
-- 出力: 月次売上集計
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monthly_sales_summary (
  target_month      CHAR(6)       NOT NULL COMMENT '対象年月 (yyyyMM)',
  store_id          VARCHAR(10)   NOT NULL COMMENT '店舗 ID',
  product_id        VARCHAR(20)   NOT NULL COMMENT '商品 ID',
  total_amount      DECIMAL(15,2) NOT NULL COMMENT '金額合計（返品は減算済み）',
  total_quantity    INT           NOT NULL COMMENT '数量合計（返品は減算済み）',
  transaction_count INT           NOT NULL COMMENT '集計対象とした取引明細の件数',
  created_at        DATETIME(3)   NOT NULL COMMENT '登録日時',
  updated_at        DATETIME(3)   NOT NULL COMMENT '更新日時',
  PRIMARY KEY (target_month, store_id, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '月次売上集計';
