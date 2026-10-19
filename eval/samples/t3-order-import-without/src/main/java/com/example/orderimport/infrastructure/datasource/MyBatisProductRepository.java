package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.repository.ProductRepository;
import com.example.orderimport.infrastructure.datasource.mapper.ProductMapper;
import org.springframework.stereotype.Repository;

/**
 * {@link ProductRepository} の MyBatis 実装。
 *
 * <p>取り込み処理からは {@link CachingProductRepository} 経由で呼ばれる。
 */
@Repository
public class MyBatisProductRepository implements ProductRepository {

    private final ProductMapper productMapper;

    public MyBatisProductRepository(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public boolean exists(ProductCode productCode) {
        return productMapper.countByProductCode(productCode.value()) > 0;
    }
}
