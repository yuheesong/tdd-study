package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointServiceConcurrencyTest {
    private static final long ANY_ID = 1L;
    private static final int THREAD_COUNT = 5;
    private static final long ANY_AMOUNT = 1000L;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserPointTable userPointTable;

    @DisplayName("포인트 충전 실패 - 1,000원 충전하는 동시 요청 5개가 들어와 총 10,000원을 초과하며 일부 실패")
    @Test
    void chargePoint_concurrency_exceedLimit() throws InterruptedException {
        long initialAmount = 8000L;

        userPointTable.insertOrUpdate(ANY_ID, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(patch("/point/{id}/charge", ANY_ID)
                                    .content(String.valueOf(ANY_AMOUNT))
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) successCount.incrementAndGet();
                    else failCount.incrementAndGet();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(3);
    }

    @DisplayName("포인트 충전 성공")
    @Test
    void chargePoint_concurrency_success() throws InterruptedException {
        long initialAmount = 0L;

        userPointTable.insertOrUpdate(ANY_ID, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/charge", ANY_ID)
                                    .content(String.valueOf(ANY_AMOUNT))
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    @DisplayName("포인트 사용 실패 - 1,000원 사용하는 동시 요청 5개가 들어와 잔액(3,000원)을 초과할 경우 일부 요청 실패")
    @Test
    void usePoint_concurrency_insufficientBalance() throws InterruptedException {
        long initialAmount = 3000L;

        userPointTable.insertOrUpdate(ANY_ID, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(patch("/point/{id}/use", ANY_ID)
                                    .content(String.valueOf(ANY_AMOUNT))
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(2);

        UserPoint userPoint = userPointTable.selectById(ANY_ID);
        assertThat(userPoint.point()).isEqualTo(0L);
    }

    @DisplayName("포인트 사용 성공")
    @Test
    void usePoint_concurrency_success() throws InterruptedException {
        long initialAmount = 10000L;

        userPointTable.insertOrUpdate(ANY_ID, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/use", ANY_ID)
                                    .content(String.valueOf(ANY_AMOUNT))
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

}
