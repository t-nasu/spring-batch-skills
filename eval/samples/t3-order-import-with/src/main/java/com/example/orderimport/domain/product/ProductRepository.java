package com.example.orderimport.domain.product;

/**
 * 商品のリポジトリ。
 */
public interface ProductRepository {

    /**
     * その商品コードの商品が存在するか。
     */
    boolean exists(ProductCode productCode);
}
