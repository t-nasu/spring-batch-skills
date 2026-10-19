package com.example.salesbatch.infrastructure.mybatis.mapper;

import com.example.salesbatch.infrastructure.mybatis.record.MonthlySalesSummaryRecord;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code monthly_sales_summary} 更新用の MyBatis マッパー。
 */
@Mapper
public interface MonthlySalesSummaryMapper {

    /**
     * 月次売上集計をまとめて登録する（キー重複時は上書き）。
     *
     * @param records 登録する行
     * @param now     登録・更新時刻
     * @return 影響行数
     */
    int upsertAll(
            @Param("records") List<MonthlySalesSummaryRecord> records,
            @Param("now") LocalDateTime now);

    /**
     * 対象年月の月次売上集計を削除する。
     *
     * @return 削除件数
     */
    int deleteByTargetMonth(@Param("targetMonth") String targetMonth);
}
