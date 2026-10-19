-- ============================================================================
-- orderImportJob が使用する業務テーブル
--
-- MySQL 8.0 を前提とした DDL だが、H2 の MySQL 互換モードでもそのまま流せる
-- 構文に揃えてある（テスト環境と本番環境で DDL を二重管理しないため）。
-- このため ENGINE 句・COLLATE 句・カラム COMMENT は使用していない。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 顧客マスタ（取り込み時の存在チェックに使用）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
  customer_code VARCHAR(20)  NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  PRIMARY KEY (customer_code)
);

-- ---------------------------------------------------------------------------
-- 商品マスタ（取り込み時の存在チェックに使用）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
  product_code VARCHAR(20)  NOT NULL,
  product_name VARCHAR(100) NOT NULL,
  PRIMARY KEY (product_code)
);

-- ---------------------------------------------------------------------------
-- 注文
--
-- business_date はジョブ引数 targetDate をそのまま保持する。注文日時
-- (ordered_at) ではなく business_date で日次集計するため、ファイルの締め時刻
-- や時差によって集計対象がぶれることがない。
--
-- order_number を主キーにしているため、同一注文番号の二重登録は
-- 一意制約違反としてジョブを異常終了させる（黙って上書きしない）。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
  order_number  VARCHAR(32)   NOT NULL,
  business_date DATE          NOT NULL,
  customer_code VARCHAR(20)   NOT NULL,
  product_code  VARCHAR(20)   NOT NULL,
  quantity      INT           NOT NULL,
  unit_price    DECIMAL(13,2) NOT NULL,
  amount        DECIMAL(15,2) NOT NULL,
  ordered_at    DATETIME      NOT NULL,
  created_at    DATETIME      NOT NULL,
  PRIMARY KEY (order_number),
  KEY idx_orders_business_date (business_date)
);

-- ---------------------------------------------------------------------------
-- 日次売上
--
-- 粒度は「業務日付 × 商品」。集計ステップは対象業務日付の行を全削除してから
-- 再投入するため、再実行しても結果が二重にならない。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_sales (
  business_date  DATE          NOT NULL,
  product_code   VARCHAR(20)   NOT NULL,
  order_count    INT           NOT NULL,
  total_quantity BIGINT        NOT NULL,
  total_amount   DECIMAL(18,2) NOT NULL,
  aggregated_at  DATETIME      NOT NULL,
  PRIMARY KEY (business_date, product_code)
);

-- ---------------------------------------------------------------------------
-- 二重起動防止ロック
--
-- ジョブ名ごとに 1 行。起動時に INSERT を試み、一意制約違反なら「既に動いて
-- いる」と判断して起動を中止する。異常終了してロック行が残った場合に備え、
-- locked_at が batch.lock.stale-after を超えた行は引き継げるようにしてある。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS batch_job_lock (
  job_name  VARCHAR(100) NOT NULL,
  owner_id  VARCHAR(100) NOT NULL,
  locked_at DATETIME     NOT NULL,
  PRIMARY KEY (job_name)
);
