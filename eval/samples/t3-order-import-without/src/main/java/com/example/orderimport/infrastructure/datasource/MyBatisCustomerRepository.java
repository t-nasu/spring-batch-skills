package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.repository.CustomerRepository;
import com.example.orderimport.infrastructure.datasource.mapper.CustomerMapper;
import org.springframework.stereotype.Repository;

/**
 * {@link CustomerRepository} の MyBatis 実装。
 *
 * <p>取り込み処理からは {@link CachingCustomerRepository} 経由で呼ばれる。
 */
@Repository
public class MyBatisCustomerRepository implements CustomerRepository {

    private final CustomerMapper customerMapper;

    public MyBatisCustomerRepository(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public boolean exists(CustomerCode customerCode) {
        return customerMapper.countByCustomerCode(customerCode.value()) > 0;
    }
}
