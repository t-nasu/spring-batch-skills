package com.example.orderimport.infrastructure.customer;

import com.example.orderimport.domain.customer.CustomerCode;
import com.example.orderimport.domain.customer.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 顧客コードの存在確認。
 *
 * <p>一度の実行で同じ顧客コードが何度も現れるため、確認した結果を覚えておき問い合わせ回数を抑える。
 * バッチは 1 回の実行で終わる短命なプロセスなので、途中で顧客が増減することは考えない。
 */
@Repository
public class MyBatisCustomerRepository implements CustomerRepository {

    /** 覚えておくコードの上限。想定を超える種類が来たときに記憶が膨らみ続けないようにする。 */
    private static final int CACHE_LIMIT = 200_000;

    private final CustomerMapper customerMapper;
    private final Map<String, Boolean> checked = new ConcurrentHashMap<>();

    public MyBatisCustomerRepository(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public boolean exists(CustomerCode customerCode) {
        Boolean remembered = checked.get(customerCode.value());
        if (remembered != null) {
            return remembered;
        }
        boolean exists = customerMapper.countByCustomerCode(customerCode.value()) > 0;
        if (checked.size() < CACHE_LIMIT) {
            checked.put(customerCode.value(), exists);
        }
        return exists;
    }
}
