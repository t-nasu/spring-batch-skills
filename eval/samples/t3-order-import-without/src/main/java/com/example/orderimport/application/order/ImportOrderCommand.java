package com.example.orderimport.application.order;

/**
 * 入力ファイルの 1 行を、解析前の文字列のまま保持したもの。
 *
 * <p>型変換も検証もここでは行わない。不正な値を検証エラーとして扱える（＝仕様に
 * ある「飛ばして続ける」ができる）ようにするため、読み取り時点では一切解釈せず、
 * 検証はすべて {@link OrderLineImportService} に集約している。
 *
 * @param lineNumber   入力ファイル上の行番号。エラー通知で行を特定するために保持する
 * @param orderNumber  注文番号
 * @param customerCode 顧客コード
 * @param productCode  商品コード
 * @param quantity     数量
 * @param unitPrice    単価
 * @param orderedAt    注文日時
 */
public record ImportOrderCommand(
        int lineNumber,
        String orderNumber,
        String customerCode,
        String productCode,
        String quantity,
        String unitPrice,
        String orderedAt) {
}
