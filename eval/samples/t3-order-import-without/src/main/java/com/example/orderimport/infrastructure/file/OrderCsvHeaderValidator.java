package com.example.orderimport.infrastructure.file;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.file.LineCallbackHandler;

/**
 * 読み飛ばしたヘッダ行が想定どおりの列構成かを確認する。
 *
 * <p>列の並びが変わったことに気づかないまま取り込むと、顧客コードと商品コードが
 * 入れ替わるなど、検証をすり抜けたまま誤ったデータが登録されてしまう。ファイル
 * 書式の変更は 1 行目で検知して即座に中断する。
 *
 * <p>ここで送出する例外はスキップ対象に含めていないため、ジョブは異常終了し
 * 運用チームへ通知される（取引先への確認が必要な事象であり、読み飛ばして続行
 * すべきものではない）。
 */
public class OrderCsvHeaderValidator implements LineCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCsvHeaderValidator.class);

    /** UTF-8 BOM。ファイル先頭に付いていることがあるため取り除く。 */
    private static final char BOM = '﻿';

    @Override
    public void handleLine(String line) {
        List<String> actual = Arrays.stream(stripBom(line).split(OrderCsvFormat.DELIMITER, -1))
                .map(String::trim)
                .toList();

        if (!OrderCsvFormat.HEADERS.equals(actual)) {
            throw new InvalidCsvHeaderException(OrderCsvFormat.HEADERS, actual);
        }
        log.debug("ヘッダ行を確認しました: {}", actual);
    }

    private static String stripBom(String line) {
        return !line.isEmpty() && line.charAt(0) == BOM ? line.substring(1) : line;
    }

    /** 入力ファイルのヘッダ行が想定と異なることを表す例外。 */
    public static class InvalidCsvHeaderException extends RuntimeException {

        public InvalidCsvHeaderException(List<String> expected, List<String> actual) {
            super("入力ファイルのヘッダ行が想定と異なります。期待=%s, 実際=%s".formatted(expected, actual));
        }
    }
}
