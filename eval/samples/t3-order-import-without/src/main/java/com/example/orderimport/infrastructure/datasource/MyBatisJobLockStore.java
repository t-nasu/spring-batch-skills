package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.application.port.JobLockStore;
import com.example.orderimport.infrastructure.datasource.dto.JobLockRow;
import com.example.orderimport.infrastructure.datasource.mapper.JobLockMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link JobLockStore} の MyBatis 実装。
 *
 * <p>ロックの獲得・解放は呼び出し元のトランザクションに巻き込まれてはならない
 * （巻き戻ったり、ジョブ本体のコミットまでロックが他プロセスから見えなかったり
 * すると二重起動を防げない）。そのため各メソッドを
 * {@link Propagation#REQUIRES_NEW} で独立したトランザクションにしている。
 *
 * <p>獲得は「INSERT して一意制約違反なら負け」という方式を採る。SELECT で確認して
 * から INSERT する方式と違い、チェックと登録の間に他プロセスが割り込む余地がない。
 */
@Repository
public class MyBatisJobLockStore implements JobLockStore {

    private final JobLockMapper jobLockMapper;

    public MyBatisJobLockStore(JobLockMapper jobLockMapper) {
        this.jobLockMapper = jobLockMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean acquire(String jobName, String ownerId, LocalDateTime lockedAt) {
        try {
            jobLockMapper.insert(jobName, ownerId, lockedAt);
            return true;
        } catch (DuplicateKeyException e) {
            // 既に他プロセスが保持している。想定内なので例外は伝播させない。
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeOlderThan(String jobName, LocalDateTime threshold) {
        return jobLockMapper.deleteOlderThan(jobName, threshold);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean release(String jobName, String ownerId) {
        return jobLockMapper.deleteByOwner(jobName, ownerId) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<JobLock> find(String jobName) {
        JobLockRow row = jobLockMapper.selectByJobName(jobName);
        return Optional.ofNullable(row)
                .map(it -> new JobLock(it.jobName(), it.ownerId(), it.lockedAt()));
    }
}
