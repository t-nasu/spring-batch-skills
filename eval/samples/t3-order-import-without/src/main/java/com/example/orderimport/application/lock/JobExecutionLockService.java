package com.example.orderimport.application.lock;

import com.example.orderimport.application.port.JobLockStore;
import com.example.orderimport.config.OrderImportProperties;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 二重起動を防ぐためのロックを扱うユースケース。
 *
 * <p>Spring Batch 自身も同一ジョブインスタンス（＝同一引数）の同時実行は拒否するが、
 * 業務日付違いで同時に走ることは止められない。注文表と日次売上表への書き込みが
 * 衝突しないよう、ジョブ名単位のロックを別に設けている。
 *
 * <p>プロセスが強制終了してロック行が残ると以降の起動が通らなくなるため、
 * {@code batch.order-import.lock-stale-after}（既定 6 時間）を超えた古いロックは
 * 異常終了の残骸とみなして引き継ぐ。起動時刻 4:00・終了期限 7:00 という運用に対して
 * 十分に長く、かつ翌日の起動までには必ず解放される長さを既定値にしている。
 */
@Service
public class JobExecutionLockService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionLockService.class);

    private final JobLockStore jobLockStore;
    private final OrderImportProperties properties;
    private final Clock clock;
    private final String ownerId;

    public JobExecutionLockService(JobLockStore jobLockStore,
                                   OrderImportProperties properties,
                                   Clock clock) {
        this.jobLockStore = jobLockStore;
        this.properties = properties;
        this.clock = clock;
        this.ownerId = currentOwnerId();
    }

    /** このプロセスを一意に表す識別子。ロックの保持者としてログと DB に残る。 */
    public String ownerId() {
        return ownerId;
    }

    /**
     * ロックを獲得する。
     *
     * @return 獲得できたら true、既に他プロセスが実行中なら false
     */
    public boolean tryAcquire(String jobName) {
        LocalDateTime now = LocalDateTime.now(clock);

        int purged = jobLockStore.purgeOlderThan(jobName, now.minus(properties.lockStaleAfter()));
        if (purged > 0) {
            log.warn("{} の古いロックを {} 件回収しました（{} 以上前に獲得されたもの）。"
                            + "前回の実行がロックを解放せずに終了した可能性があります",
                    jobName, purged, properties.lockStaleAfter());
        }

        if (jobLockStore.acquire(jobName, ownerId, now)) {
            log.info("{} のロックを獲得しました (owner={})", jobName, ownerId);
            return true;
        }

        Optional<JobLockStore.JobLock> holder = jobLockStore.find(jobName);
        log.error("{} は既に実行中のため起動を中止します。保持者: {}", jobName,
                holder.map(lock -> "%s (獲得時刻=%s)".formatted(lock.ownerId(), lock.lockedAt()))
                        .orElse("(不明)"));
        return false;
    }

    /**
     * ロックを解放する。
     *
     * <p>ジョブの成否にかかわらず呼び出されるため、解放できなくても例外は送出せず
     * 記録に留める。残ったロックは次回起動時の回収処理で取り除かれる。
     */
    public void release(String jobName) {
        try {
            if (jobLockStore.release(jobName, ownerId)) {
                log.info("{} のロックを解放しました (owner={})", jobName, ownerId);
            } else {
                log.warn("{} のロックを解放できませんでした (owner={})。"
                        + "他プロセスに引き継がれた可能性があります", jobName, ownerId);
            }
        } catch (RuntimeException e) {
            log.error("{} のロック解放に失敗しました (owner={})", jobName, ownerId, e);
        }
    }

    private static String currentOwnerId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }
        return host + ":" + ManagementFactory.getRuntimeMXBean().getPid();
    }
}
