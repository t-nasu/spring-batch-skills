package com.example.orderimport.infrastructure.datasource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code customers} へのアクセス。
 */
@Mapper
public interface CustomerMapper {

    @Select("SELECT COUNT(*) FROM customers WHERE customer_code = #{customerCode}")
    int countByCustomerCode(@Param("customerCode") String customerCode);
}
