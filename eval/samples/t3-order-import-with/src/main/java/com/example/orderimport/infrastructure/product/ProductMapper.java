package com.example.orderimport.infrastructure.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * products 表への Mapper。SQL は ProductMapper.xml に書く。
 */
@Mapper
public interface ProductMapper {

    int countByProductCode(@Param("productCode") String productCode);
}
