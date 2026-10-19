package com.example.expirepoints.domain;

/** 失効できない会員ポイントを失効させようとしたことを表す。 */
public class PointNotExpirableException extends RuntimeException {

    public PointNotExpirableException(String message) {
        super(message);
    }
}
