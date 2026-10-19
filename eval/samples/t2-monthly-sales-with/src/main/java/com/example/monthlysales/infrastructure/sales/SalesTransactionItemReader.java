package com.example.monthlysales.infrastructure.sales;

import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.TargetMonth;
import java.util.Map;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.InitializingBean;

/**
 * 対象年月の取引明細を 1 件ずつ読み出す Reader。
 *
 * <p>読み出す範囲は「取引日が対象年月の初日以上、翌月の初日未満」である。
 * 読み出し順は取引日・取引 ID の昇順に固定してあり、何度実行しても同じ順序になる。
 * Spring Batch は読み出した件数を記録しているため、途中で失敗しても
 * 同じ順序で読み飛ばして中断した位置から再開できる。
 *
 * <p>読み取りだけの大量処理のため、リポジトリを介さず Mapper の文を直接使う。
 * テーブル形 DTO からドメインオブジェクトへの変換もここで行うので、
 * 後続の Processor には常に {@link SalesTransaction} が渡る。
 */
public class SalesTransactionItemReader
        implements ItemStreamReader<SalesTransaction>, InitializingBean {

    private final MyBatisCursorItemReader<SalesTransactionRow> delegate;

    public SalesTransactionItemReader(SqlSessionFactory sqlSessionFactory, TargetMonth targetMonth) {
        this.delegate = new MyBatisCursorItemReader<>();
        this.delegate.setName("salesTransactionCursor");
        this.delegate.setSqlSessionFactory(sqlSessionFactory);
        this.delegate.setQueryId(SalesTransactionMapper.SELECT_BY_TRANSACTION_DATE_RANGE);
        this.delegate.setParameterValues(Map.of(
                "fromDate", targetMonth.firstDay(),
                "toDateExclusive", targetMonth.firstDayOfNextMonth()));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        delegate.afterPropertiesSet();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public SalesTransaction read() throws Exception {
        SalesTransactionRow row = delegate.read();
        return row == null ? null : toDomain(row);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    private SalesTransaction toDomain(SalesTransactionRow row) {
        return SalesTransaction.create(
                row.transactionId(),
                row.storeId(),
                row.productId(),
                row.transactionDate(),
                row.type(),
                row.amount(),
                row.quantity());
    }
}
