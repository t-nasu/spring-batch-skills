package com.example.orderimport.presentation.runner;

/**
 * プロセスの終了コード。
 *
 * <p>ジョブスケジューラ側で「再実行すべきか」「人が見るべきか」を切り分けられる
 * よう、失敗の種類ごとに値を分けている。
 */
public enum JobExitCode {

    /** 正常終了。 */
    SUCCESS(0),

    /** 取り込みまたは集計が失敗した。運用チームへ通知済み。同じ引数で再実行すれば再開する。 */
    JOB_FAILED(1),

    /** 同じジョブが既に実行中のため起動しなかった。何も処理していない。 */
    ALREADY_RUNNING(2),

    /** ジョブ引数が不正。何も処理していない。 */
    INVALID_ARGUMENTS(3),

    /** 指定された業務日付の取り込みは既に正常終了している。何も処理していない。 */
    ALREADY_COMPLETED(4);

    private final int value;

    JobExitCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
