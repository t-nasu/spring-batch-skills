package com.example.monthlysales.domain.sales;

import java.util.Optional;

/** 取引の区分。 */
public enum TransactionType {

    /** 売上。集計に足し込む。 */
    SALE(false),

    /** 返品。集計から差し引く。 */
    RETURN(true);

    private final boolean subtracts;

    TransactionType(boolean subtracts) {
        this.subtracts = subtracts;
    }

    /** 集計から差し引く区分か。返品だけが真である。 */
    public boolean subtracts() {
        return subtracts;
    }

    /** DB に入っている区分コードから組み立てる。未知のコードは空を返す。 */
    public static Optional<TransactionType> from(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.strip();
        for (TransactionType type : values()) {
            if (type.name().equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
