package com.example.pointbatch.presentation.expirepoints;

/**
 * 起動引数が不正であることを表す例外。
 */
public class JobArgumentException extends RuntimeException {

    public JobArgumentException(String message) {
        super(message);
    }

    public JobArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
