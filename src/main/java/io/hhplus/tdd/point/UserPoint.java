package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        long maxPoint = 10_000L;
        long newPoint = this.point + amount;

        if (newPoint > maxPoint) {
            throw new IllegalArgumentException("포인트는 최대 10,000원까지 충전 가능합니다.");
        }
        return new UserPoint(id, newPoint,  System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        if (this.point < amount) {
            throw new IllegalStateException("보유 포인트가 부족합니다.");
        }

        long newPoint = this.point - amount;

        return new UserPoint(id, newPoint,  System.currentTimeMillis());
    }
}
