package com.example.orderimport.infrastructure.datasource.mapper;

import com.example.orderimport.infrastructure.datasource.dto.DailySalesRow;
import com.example.orderimport.infrastructure.datasource.dto.OrderRow;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code orders} へのアクセス。SQL は同パッケージの OrderMapper.xml に置く。
 */
@Mapper
public interface OrderMapper {

    /**
     * 複数行を 1 文の INSERT で登録する。
     *
     * <p>最大 30 万件を 7:00 までに取り込む必要があるため、1 行ずつの INSERT ではなく
     * 複数行 INSERT でラウンドトリップを削減している。
     *
     * @param orders 登録する行。呼び出し側で空でないことを保証すること
     */
    void insertAll(@Param("orders") List<OrderRow> orders);

    /** 指定業務日付の注文を商品単位に集計する。 */
    List<DailySalesRow> aggregateDailySales(@Param("businessDate") LocalDate businessDate);
}
