package com.example.orderimport.infrastructure.customer;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * customers 表への Mapper。SQL は CustomerMapper.xml に書く。
 */
@Mapper
public interface CustomerMapper {

    int countByCustomerCode(@Param("customerCode") String customerCode);
}
