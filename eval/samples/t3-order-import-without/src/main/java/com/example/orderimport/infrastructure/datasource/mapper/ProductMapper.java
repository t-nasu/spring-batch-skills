package com.example.orderimport.infrastructure.datasource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code products} へのアクセス。
 */
@Mapper
public interface ProductMapper {

    @Select("SELECT COUNT(*) FROM products WHERE product_code = #{productCode}")
    int countByProductCode(@Param("productCode") String productCode);
}
