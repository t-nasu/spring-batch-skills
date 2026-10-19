package com.example.orderimport.infrastructure.product;

import com.example.orderimport.domain.product.ProductCode;
import com.example.orderimport.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品コードの存在確認。
 *
 * <p>顧客コードと同じ理由で、確認した結果を覚えておき問い合わせ回数を抑える。
 */
@Repository
public class MyBatisProductRepository implements ProductRepository {

    /** 覚えておくコードの上限。 */
    private static final int CACHE_LIMIT = 200_000;

    private final ProductMapper productMapper;
    private final Map<String, Boolean> checked = new ConcurrentHashMap<>();

    public MyBatisProductRepository(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public boolean exists(ProductCode productCode) {
        Boolean remembered = checked.get(productCode.value());
        if (remembered != null) {
            return remembered;
        }
        boolean exists = productMapper.countByProductCode(productCode.value()) > 0;
        if (checked.size() < CACHE_LIMIT) {
            checked.put(productCode.value(), exists);
        }
        return exists;
    }
}
