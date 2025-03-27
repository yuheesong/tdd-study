package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    private static final long ANY_ID = 1L;
    private static final long ANY_AMOUNT = 5000L;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;
    @InjectMocks
    private PointService pointService;
    UserPoint userPoint;

    @Test
    @DisplayName("포인트 조회 실패 - 메서드가 존재하지 않아 컴파일 조차 실패합니다.")
    void getUserPoint_emptyMethod() {
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());

        assertThatThrownBy(() -> pointService.getUserPoint(3L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포인트 조회 실패 - 데이터가 존재하지 않아 실패합니다.")
    void getUserPoint_emptyData() {
        when(userPointTable.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> pointService.getUserPoint(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포인트 조회 성공")
    void getUserPoint_success() {
        // given
        UserPoint expected = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());
        when(userPointTable.selectById(ANY_ID)).thenReturn(expected);

        // when
        UserPoint result = pointService.getUserPoint(ANY_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ANY_ID);
        assertThat(result.point()).isEqualTo(ANY_AMOUNT);

    }

    @Test
    @DisplayName("포인트 충전 실패 - 포인트는 총 10,000을 넘을 수 없습니다.")
    void chargePoint_exceedLimit() {
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());
        assertThatThrownBy(() -> userPoint.charge(8000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 충전 포인트는 0보다 커야합니다.")
    void chargePoint_invalidAmount() {
        assertThatThrownBy(() -> pointService.charge(ANY_ID, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_success() {
        // given
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());

        // when
        UserPoint charged = userPoint.charge(500L);

        // then
        assertThat(charged.id()).isEqualTo(ANY_ID);
        assertThat(charged.point()).isEqualTo(ANY_AMOUNT + 500L);
    }

    @Test
    @DisplayName("포인트 사용 실패 - 사용 포인트는 잔액을 넘길 수 없습니다.")
    void usePoint_overBalance() {
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());
        assertThatThrownBy(() -> userPoint.use(8000L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("포인트 사용 실패 - 사용 포인트는 0보다 커야 합니다.")
    void usePoint_invalidPoint() {
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());
        assertThatThrownBy(() -> pointService.use(ANY_ID,0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_success() {
        // given
        userPoint = new UserPoint(ANY_ID, ANY_AMOUNT, System.currentTimeMillis());

        // when
        UserPoint result = userPoint.use(1000L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ANY_ID);
        assertThat(result.point()).isEqualTo(ANY_AMOUNT - 1000L);

    }

    @Test
    @DisplayName("포인트 내역 조회 실패 - 데이터가 존재하지 않아 실패합니다.")
    void getUserPointHistory_emptyData() {
        when(pointHistoryTable.selectAllByUserId(999L)).thenReturn(null);

        assertThatThrownBy(() -> pointService.getPointHistory(999L))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @DisplayName("포인트 내역 조회 성공")
    void getUserPointHistory_success() {
        // given
        PointHistory expected = new PointHistory(1, ANY_ID, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory expected2 = new PointHistory(2, ANY_ID, 500L, TransactionType.USE, System.currentTimeMillis());

        List<PointHistory> mockHistoryList = List.of(expected, expected2);

        when(pointHistoryTable.selectAllByUserId(ANY_ID)).thenReturn(mockHistoryList);

        // when
        List<PointHistory> result = pointService.getPointHistory(ANY_ID);

        // then
        assertThat(result).asList().hasSize(2);
        assertThat(result.get(0)).usingRecursiveComparison().isEqualTo(expected);
        assertThat(result.get(1)).usingRecursiveComparison().isEqualTo(expected2);
    }
}
