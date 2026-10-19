package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.repository.CustomerRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * 顧客の存在チェック結果をプロセス内に覚えておく {@link CustomerRepository}。
 *
 * <p>1 行ごとにマスタへ問い合わせると最大 30 万回の往復が発生し、終了期限 7:00 に
 * 対して無視できない負荷になる。同じ顧客コードは何度も現れるため、一度確かめた
 * 結果を使い回す。
 *
 * <p>バッチは 1 回の実行で終了する前提なので、キャッシュの有効期間はプロセスの
 * 寿命と同じでよい（実行中にマスタが更新されても追従しない）。無制限に膨らむのを
 * 避けるため上限を設け、超えた分はキャッシュせず都度問い合わせる。
 */
@Primary
@Repository
public class CachingCustomerRepository implements CustomerRepository {

    /** 顧客マスタの想定規模を上回る値。これを超えたら以降はキャッシュしない。 */
    private static final int MAX_ENTRIES = 500_000;

    private final CustomerRepository delegate;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public CachingCustomerRepository(MyBatisCustomerRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean exists(CustomerCode customerCode) {
        Boolean cached = cache.get(customerCode.value());
        if (cached != null) {
            return cached;
        }
        boolean exists = delegate.exists(customerCode);
        if (cache.size() < MAX_ENTRIES) {
            cache.put(customerCode.value(), exists);
        }
        return exists;
    }
}
