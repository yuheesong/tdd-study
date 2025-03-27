package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final Map<Long, Object> userLocks = new ConcurrentHashMap<>();

    /**
     * 특정 유저의 포인트를 조회하는 기능
     */
    public UserPoint getUserPoint(Long userId) {
        UserPoint userPoint;

        userPoint = userPointTable.selectById(userId);
        if (userPoint == null) {
            throw new RuntimeException("해당 유저 데이터를 찾을 수 없습니다.");
        }

        return userPoint;
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     */
    public List<PointHistory> getPointHistory(Long userId) {
        List<PointHistory> pointHistory = pointHistoryTable.selectAllByUserId(userId);

        if (pointHistory == null || pointHistory.isEmpty()) {
            throw new RuntimeException("해당 유저의 포인트 내역이 존재하지 않습니다.");
        }

        return pointHistory;

    }


    /**
     * 특정 유저의 포인트를 충전하는 기능
     */
    public UserPoint charge(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        Object lock = userLocks.computeIfAbsent(userId, id -> new Object());

        synchronized (lock) {
            UserPoint userPoint = userPointTable.selectById(userId);

            UserPoint charge = userPoint.charge(amount);
            UserPoint updated = userPointTable.insertOrUpdate(userId, charge.point());

            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, updated.updateMillis());
            return updated;
        }
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     */
    public UserPoint use(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        Object lock = userLocks.computeIfAbsent(userId, id -> new Object());

        synchronized (lock) {
            UserPoint userPoint = userPointTable.selectById(userId);
            if (userPoint.point() < amount) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }

            UserPoint updated = userPoint.use(amount);
            userPointTable.insertOrUpdate(userId, updated.point());
            pointHistoryTable.insert(userId, amount, TransactionType.USE, updated.updateMillis());

            return updated;
        }
    }

}
