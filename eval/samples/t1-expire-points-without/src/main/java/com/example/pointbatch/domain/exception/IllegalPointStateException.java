package com.example.pointbatch.domain.exception;

/**
 * ポイントが要求された操作を受け付けられる状態にないことを表す例外。
 */
public class IllegalPointStateException extends DomainException {

    public IllegalPointStateException(String message) {
        super(message);
    }
}
