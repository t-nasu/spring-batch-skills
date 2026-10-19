package com.example.monthlysales.domain.summary;

import com.example.monthlysales.domain.shared.ProductId;
import com.example.monthlysales.domain.shared.StoreId;
import com.example.monthlysales.domain.shared.TargetMonth;
import java.util.Objects;

/** 月次売上集計を一意に定める識別子。対象年月・店舗・商品の組である。 */
public record MonthlySalesKey(TargetMonth targetMonth, StoreId storeId, ProductId productId) {

    public MonthlySalesKey {
        Objects.requireNonNull(targetMonth, "対象年月が無い");
        Objects.requireNonNull(storeId, "店舗 ID が無い");
        Objects.requireNonNull(productId, "商品 ID が無い");
    }

    @Override
    public String toString() {
        return "[" + targetMonth + "/" + storeId + "/" + productId + "]";
    }
}
