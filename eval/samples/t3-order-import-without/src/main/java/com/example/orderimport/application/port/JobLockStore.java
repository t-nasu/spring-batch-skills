package com.example.orderimport.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 二重起動防止ロックの永続化ポート。
 *
 * <p>複数ホストから同じジョブが起動されても一方だけが通るよう、ロックは
 * プロセス内ではなく共有 DB 上で管理する。
 *
 * <p>各メソッドは呼び出し元のトランザクションから独立して即座に確定しなければ
 * ならない（ロックを取ったことが他プロセスから直ちに見える必要があるため）。
 */
public interface JobLockStore {

    /**
     * ロックを獲得する。
     *
     * @param jobName  ジョブ名
     * @param ownerId  獲得者の識別子（ホスト名とプロセス ID）
     * @param lockedAt 獲得時刻
     * @return 獲得できたら true、既に他プロセスが保持していたら false
     */
    boolean acquire(String jobName, String ownerId, LocalDateTime lockedAt);

    /**
     * 指定時刻より前に獲得された古いロックを削除する。
     *
     * <p>プロセスが強制終了してロック行が残った場合の回収用。
     *
     * @return 削除件数
     */
    int purgeOlderThan(String jobName, LocalDateTime threshold);

    /**
     * ロックを解放する。
     *
     * <p>{@code ownerId} が一致する行だけを削除する。他プロセスが獲得し直した
     * ロックを誤って解放しないため。
     *
     * @return 解放できたら true
     */
    boolean release(String jobName, String ownerId);

    /** 現在のロック。保持者がいなければ空。 */
    Optional<JobLock> find(String jobName);

    /**
     * 獲得されているロック。
     *
     * @param jobName  ジョブ名
     * @param ownerId  保持者の識別子
     * @param lockedAt 獲得時刻
     */
    record JobLock(String jobName, String ownerId, LocalDateTime lockedAt) {
    }
}
