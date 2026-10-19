package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.repository.ProductRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

/**
 * 商品の存在チェック結果をプロセス内に覚えておく {@link ProductRepository}。
 *
 * <p>意図は {@link CachingCustomerRepository} と同じ。
 */
@Primary
@Repository
public class CachingProductRepository implements ProductRepository {

    /** 商品マスタの想定規模を上回る値。これを超えたら以降はキャッシュしない。 */
    private static final int MAX_ENTRIES = 500_000;

    private final ProductRepository delegate;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public CachingProductRepository(MyBatisProductRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean exists(ProductCode productCode) {
        Boolean cached = cache.get(productCode.value());
        if (cached != null) {
            return cached;
        }
        boolean exists = delegate.exists(productCode);
        if (cache.size() < MAX_ENTRIES) {
            cache.put(productCode.value(), exists);
        }
        return exists;
    }
}
