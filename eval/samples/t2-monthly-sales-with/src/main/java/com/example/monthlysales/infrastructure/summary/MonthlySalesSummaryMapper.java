package com.example.monthlysales.infrastructure.summary;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** monthly_sales_summary 表を更新する Mapper。SQL は MonthlySalesSummaryMapper.xml にある。 */
@Mapper
public interface MonthlySalesSummaryMapper {

    /**
     * 渡された行を加算登録する。
     * 同じ主キーの行があれば金額と数量を足し込み、無ければ新しく登録する。
     */
    void addAll(@Param("rows") List<MonthlySalesSummaryRow> rows);
}
