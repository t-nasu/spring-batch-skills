package com.example.orderimport.presentation.job;

/**
 * ジョブ引数の名前。
 */
public final class JobParameterNames {

    /**
     * 業務日付（{@code yyyyMMdd}）。
     *
     * <p>ジョブインスタンスを識別する引数。同じ業務日付で再実行すると、前回失敗した
     * 実行の続きから再開する。
     */
    public static final String TARGET_DATE = "targetDate";

    /**
     * 入力ファイルのパス。
     *
     * <p>ジョブインスタンスの識別には使わない（非識別引数）。「業務日付 X の取り込み」
     * という 1 つの仕事を、ファイルの置き場所が変わっても同じインスタンスとして
     * 扱い続けられるようにするため。再開時は新しく指定されたパスが使われるので、
     * 同じ内容のファイルを指すこと。
     */
    public static final String INPUT_FILE = "inputFile";

    private JobParameterNames() {
    }
}
