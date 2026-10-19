package com.example.pointbatch.presentation;

/**
 * バッチの終了コード。スケジューラはこの値で成否を判定する。
 */
public enum ExitCode {

    /** 正常終了。 */
    SUCCESS(0),

    /** 異常終了(処理中のエラー)。 */
    FAILED(1),

    /** 起動引数が不正。 */
    INVALID_ARGUMENT(2);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
