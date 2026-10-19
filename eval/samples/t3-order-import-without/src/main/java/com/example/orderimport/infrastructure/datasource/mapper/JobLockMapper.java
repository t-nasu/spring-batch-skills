package com.example.orderimport.infrastructure.datasource.mapper;

import com.example.orderimport.infrastructure.datasource.dto.JobLockRow;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code batch_job_lock} へのアクセス。
 */
@Mapper
public interface JobLockMapper {

    /**
     * ロック行を登録する。既に行があれば一意制約違反となる。
     */
    @Insert("""
            INSERT INTO batch_job_lock (job_name, owner_id, locked_at)
            VALUES (#{jobName}, #{ownerId}, #{lockedAt})
            """)
    int insert(@Param("jobName") String jobName,
               @Param("ownerId") String ownerId,
               @Param("lockedAt") LocalDateTime lockedAt);

    @Delete("DELETE FROM batch_job_lock WHERE job_name = #{jobName} AND locked_at < #{threshold}")
    int deleteOlderThan(@Param("jobName") String jobName,
                        @Param("threshold") LocalDateTime threshold);

    @Delete("DELETE FROM batch_job_lock WHERE job_name = #{jobName} AND owner_id = #{ownerId}")
    int deleteByOwner(@Param("jobName") String jobName, @Param("ownerId") String ownerId);

    @Select("""
            SELECT job_name AS jobName, owner_id AS ownerId, locked_at AS lockedAt
            FROM batch_job_lock
            WHERE job_name = #{jobName}
            """)
    JobLockRow selectByJobName(@Param("jobName") String jobName);
}
