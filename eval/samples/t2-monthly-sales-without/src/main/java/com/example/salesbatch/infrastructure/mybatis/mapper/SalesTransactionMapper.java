package com.example.salesbatch.infrastructure.mybatis.mapper;

import com.example.salesbatch.infrastructure.mybatis.record.SalesTransactionRecord;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * {@code sales_transaction} 参照用の MyBatis マッパー。
 */
@Mapper
public interface SalesTransactionMapper {

    /**
     * 取引日が {@code [from, toExclusive)} の範囲にある取引明細を
     * {@code (店舗 ID, 商品 ID, 取引 ID)} 昇順で取得する。
     *
     * <p>{@code afterStoreId} 以降の引数は読み込み開始位置（キーセットページング）。</p>
     *
     * @param from               取引日の下限（この日を含む）
     * @param toExclusive        取引日の上限（この日を含まない）
     * @param afterStoreId       この店舗 ID より後ろから読む（{@code null} なら先頭から）
     * @param afterProductId     {@code afterStoreId} と同じ店舗ではこの商品 ID より後ろから読む
     * @param afterTransactionId 同じ店舗・商品ではこの取引 ID より後ろから読む
     *                           （{@code null} なら当該の店舗・商品は読み飛ばす）
     * @param limit              取得件数の上限
     */
    List<SalesTransactionRecord> selectPage(
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("afterStoreId") String afterStoreId,
            @Param("afterProductId") String afterProductId,
            @Param("afterTransactionId") Long afterTransactionId,
            @Param("limit") int limit);
}
