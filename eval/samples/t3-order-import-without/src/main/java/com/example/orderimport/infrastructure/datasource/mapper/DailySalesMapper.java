package com.example.orderimport.infrastructure.datasource.mapper;

import com.example.orderimport.infrastructure.datasource.dto.DailySalesRow;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code daily_sales} へのアクセス。SQL は同パッケージの DailySalesMapper.xml に置く。
 */
@Mapper
public interface DailySalesMapper {

    int deleteByBusinessDate(@Param("businessDate") LocalDate businessDate);

    /**
     * 複数行を 1 文の INSERT で登録する。
     *
     * @param rows 登録する行。呼び出し側で空でないことを保証すること
     */
    void insertAll(@Param("rows") List<DailySalesRow> rows);

    List<DailySalesRow> findByBusinessDate(@Param("businessDate") LocalDate businessDate);
}
