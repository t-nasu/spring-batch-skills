package com.example.pointbatch.application.expirepoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.pointbatch.domain.exception.PointUpdateConflictException;
import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class ExpirePointsServiceTest {

    private static final BusinessDate BUSINESS_DATE = BusinessDate.of(LocalDate.of(2026, 4, 1));
    private static final int CHUNK_SIZE = 3;

    private final ExpirePointsChunkProcessor chunkProcessor = mock(ExpirePointsChunkProcessor.class);
    private final ExpirePointsService service =
            new ExpirePointsService(chunkProcessor, new ExpirePointsProperties(CHUNK_SIZE));

    @Test
    @DisplayName("対象が無い場合は 0 件で正常終了する")
    void noTargets() {
        when(chunkProcessor.process(eq(BUSINESS_DATE), eq(Optional.empty()), eq(CHUNK_SIZE)))
                .thenReturn(ExpirePointsChunkResult.empty());

        ExpirePointsResult result = service.execute(BUSINESS_DATE);

        assertThat(result).isEqualTo(new ExpirePointsResult(0L, 0L, 0));
        verify(chunkProcessor, times(1)).process(any(), any(), anyInt());
    }

    @Test
    @DisplayName("対象が無くなるまでカーソルを進めながらチャンクを繰り返す")
    void iteratesChunksWithCursor() {
        when(chunkProcessor.process(BUSINESS_DATE, Optional.empty(), CHUNK_SIZE))
                .thenReturn(new ExpirePointsChunkResult(3, 300L, Optional.of(MemberPointId.of(3L))));
        when(chunkProcessor.process(BUSINESS_DATE, Optional.of(MemberPointId.of(3L)), CHUNK_SIZE))
                .thenReturn(new ExpirePointsChunkResult(3, 60L, Optional.of(MemberPointId.of(9L))));
        when(chunkProcessor.process(BUSINESS_DATE, Optional.of(MemberPointId.of(9L)), CHUNK_SIZE))
                .thenReturn(new ExpirePointsChunkResult(1, 5L, Optional.of(MemberPointId.of(11L))));

        ExpirePointsResult result = service.execute(BUSINESS_DATE);

        assertThat(result).isEqualTo(new ExpirePointsResult(7L, 365L, 3));

        InOrder inOrder = Mockito.inOrder(chunkProcessor);
        inOrder.verify(chunkProcessor).process(BUSINESS_DATE, Optional.empty(), CHUNK_SIZE);
        inOrder.verify(chunkProcessor).process(BUSINESS_DATE, Optional.of(MemberPointId.of(3L)), CHUNK_SIZE);
        inOrder.verify(chunkProcessor).process(BUSINESS_DATE, Optional.of(MemberPointId.of(9L)), CHUNK_SIZE);
        // 最終チャンクが chunkSize 未満なので追加の問い合わせは行わない
        verifyNoMoreInteractions(chunkProcessor);
    }

    @Test
    @DisplayName("チャンクが chunkSize ちょうどで終わった場合は空になるまで問い合わせる")
    void queriesUntilEmptyWhenLastChunkIsFull() {
        when(chunkProcessor.process(BUSINESS_DATE, Optional.empty(), CHUNK_SIZE))
                .thenReturn(new ExpirePointsChunkResult(3, 30L, Optional.of(MemberPointId.of(3L))));
        when(chunkProcessor.process(BUSINESS_DATE, Optional.of(MemberPointId.of(3L)), CHUNK_SIZE))
                .thenReturn(ExpirePointsChunkResult.empty());

        ExpirePointsResult result = service.execute(BUSINESS_DATE);

        assertThat(result).isEqualTo(new ExpirePointsResult(3L, 30L, 1));
        verify(chunkProcessor, times(2)).process(any(), any(), anyInt());
    }

    @Test
    @DisplayName("チャンク処理の例外はそのまま伝播させ、ジョブを止める")
    void propagatesException() {
        when(chunkProcessor.process(BUSINESS_DATE, Optional.empty(), CHUNK_SIZE))
                .thenThrow(new PointUpdateConflictException("conflict"));

        assertThatThrownBy(() -> service.execute(BUSINESS_DATE))
                .isInstanceOf(PointUpdateConflictException.class);
    }

    @Test
    @DisplayName("chunk-size の既定値は 1000")
    void defaultChunkSize() {
        assertThat(new ExpirePointsProperties(null).chunkSize()).isEqualTo(1000);
        assertThatThrownBy(() -> new ExpirePointsProperties(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
