package com.example.monthlysales.infrastructure.sales;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

/** sales_transaction 表を読む Mapper。SQL は SalesTransactionMapper.xml にある。 */
@Mapper
public interface SalesTransactionMapper {

    /** カーソル読み込みの Reader に渡す文の ID。 */
    String SELECT_BY_TRANSACTION_DATE_RANGE =
            "com.example.monthlysales.infrastructure.sales.SalesTransactionMapper"
                    + ".selectByTransactionDateRange";

    /**
     * 取引日が {@code fromDate} 以上 {@code toDateExclusive} 未満の取引明細を、
     * 取引日・取引 ID の昇順で読み出す。
     */
    Cursor<SalesTransactionRow> selectByTransactionDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDateExclusive") LocalDate toDateExclusive);
}
