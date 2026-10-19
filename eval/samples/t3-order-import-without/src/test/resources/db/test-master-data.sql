-- 検証用の顧客マスタ・商品マスタ。
-- 取り込み対象の CSV は、ここに無いコードを「マスタに存在しない」として弾かれる。

INSERT INTO customers (customer_code, customer_name) VALUES
    ('C001', '株式会社あかつき商事'),
    ('C002', '有限会社いろは物産'),
    ('C003', 'うみのこ株式会社');

INSERT INTO products (product_code, product_name) VALUES
    ('P001', 'A4 コピー用紙 500 枚'),
    ('P002', '油性ボールペン 黒'),
    ('P003', 'クラフト封筒 角 2');
