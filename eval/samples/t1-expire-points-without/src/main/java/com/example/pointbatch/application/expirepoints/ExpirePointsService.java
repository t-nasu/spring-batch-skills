package com.example.pointbatch.application.expirepoints;

import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ポイント失効ジョブのユースケース実装。
 *
 * <p>チャンク単位の処理({@link ExpirePointsChunkProcessor})を対象が無くなるまで繰り返す。
 * 処理済みの会員ポイント ID をカーソルとして進めるため、同じ行を再取得することはない。
 */
@Service
public class ExpirePointsService implements ExpirePointsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpirePointsService.class);

    private final ExpirePointsChunkProcessor chunkProcessor;
    private final int chunkSize;

    public ExpirePointsService(
            ExpirePointsChunkProcessor chunkProcessor, ExpirePointsProperties properties) {
        this.chunkProcessor = chunkProcessor;
        this.chunkSize = properties.chunkSize();
    }

    @Override
    public ExpirePointsResult execute(BusinessDate businessDate) {
        Objects.requireNonNull(businessDate, "業務日付は必須です");

        Optional<MemberPointId> cursor = Optional.empty();
        long expiredPointCount = 0L;
        long expiredAmountTotal = 0L;
        int chunkCount = 0;

        while (true) {
            ExpirePointsChunkResult chunk = chunkProcessor.process(businessDate, cursor, chunkSize);
            if (chunk.isEmpty()) {
                break;
            }

            expiredPointCount += chunk.processedCount();
            expiredAmountTotal += chunk.amountTotal();
            chunkCount++;
            cursor = chunk.lastProcessedId();

            log.info(
                    "チャンクを失効しました: chunkNo={}, count={}, lastMemberPointId={}, 累計={}",
                    chunkCount,
                    chunk.processedCount(),
                    cursor.map(MemberPointId::value).orElse(null),
                    expiredPointCount);

            // 取得件数が上限未満なら、これ以上の対象は存在しない。
            if (chunk.processedCount() < chunkSize) {
                break;
            }
        }

        return new ExpirePointsResult(expiredPointCount, expiredAmountTotal, chunkCount);
    }
}
