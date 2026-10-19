-- 業務日付 20261019 で動かしたときの境界を網羅する。
INSERT INTO member_point (id, member_id, points, expires_on, status) VALUES
    (1, 100, 50, DATE '2026-10-18', 'ACTIVE'),   -- 期限切れ。失効させる
    (2, 100, 30, DATE '2026-01-01', 'ACTIVE'),   -- だいぶ前に期限切れ。失効させる
    (3, 200, 20, DATE '2026-10-19', 'ACTIVE'),   -- 期限当日。まだ使えるので残す
    (4, 200, 10, DATE '2026-10-20', 'ACTIVE'),   -- 期限前。残す
    (5, 300, 40, DATE '2026-05-01', 'EXPIRED');  -- すでに失効済み。触らない
