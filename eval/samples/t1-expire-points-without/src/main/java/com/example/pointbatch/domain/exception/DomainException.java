package com.example.pointbatch.domain.exception;

/**
 * ドメイン層で検出した業務的な異常を表す基底例外。
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
