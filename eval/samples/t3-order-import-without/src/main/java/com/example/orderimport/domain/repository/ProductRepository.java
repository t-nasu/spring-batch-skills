package com.example.orderimport.domain.repository;

import com.example.orderimport.domain.model.order.ProductCode;

/**
 * 商品マスタのリポジトリ。
 *
 * <p>取り込み処理では存在チェックしか行わないため、商品エンティティ全体ではなく
 * 存在有無だけを返す。
 */
public interface ProductRepository {

    boolean exists(ProductCode productCode);
}
