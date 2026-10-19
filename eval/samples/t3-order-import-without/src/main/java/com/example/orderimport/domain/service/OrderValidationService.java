package com.example.orderimport.domain.service;

import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.repository.CustomerRepository;
import com.example.orderimport.domain.repository.ProductRepository;
import com.example.orderimport.domain.validation.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 注文が成立するための、他集約を参照する検証を担うドメインサービス。
 *
 * <p>単独の値の妥当性（書式、数量が 1 以上）は各値オブジェクトが自ら守るため、
 * ここでは「顧客マスタ・商品マスタに実在すること」だけを判定する。
 */
@Service
public class OrderValidationService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderValidationService(CustomerRepository customerRepository,
                                  ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /**
     * 顧客・商品がマスタに実在するかを検証する。
     *
     * @return 検証エラーのリスト。問題がなければ空リスト
     */
    public List<ValidationError> validateMasterReferences(CustomerCode customerCode,
                                                          ProductCode productCode) {
        List<ValidationError> errors = new ArrayList<>(2);
        if (!customerRepository.exists(customerCode)) {
            errors.add(ValidationError.of("顧客コード", "顧客マスタに存在しません: " + customerCode));
        }
        if (!productRepository.exists(productCode)) {
            errors.add(ValidationError.of("商品コード", "商品マスタに存在しません: " + productCode));
        }
        return errors;
    }
}
