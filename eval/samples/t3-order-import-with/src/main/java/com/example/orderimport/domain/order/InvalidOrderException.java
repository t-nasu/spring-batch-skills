package com.example.orderimport.domain.order;

/**
 * 注文が取り込みの条件を満たしていないことを表す。
 * この例外が出た行は読み飛ばす（上限は batch 層の設定による）。
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
