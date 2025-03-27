package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointServiceIntegrationTest {
    private static final long ANY_ID = 1L;
    private static final long INITIAL_AMOUNT = 5000L;
    private static final long ANY_AMOUNT = 1000L;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserPointTable userPointTable;
    @MockBean
    private PointHistoryTable pointHistoryTable;

    @Test
    @DisplayName("포인트 조회 실패 - 존재하지 않는 유저")
    void getUserPoint_userNotFound() throws Exception {
        // given
        long nonExistentUserId = 9999L;

        when(userPointTable.selectById(nonExistentUserId)).thenReturn(null);

        // when & then
        mockMvc.perform(get("/point/{id}", String.valueOf(nonExistentUserId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("해당 유저 데이터를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("포인트 조회 성공")
    void getUserPoint_success() throws Exception {
        // given
        UserPoint userPoint = new UserPoint(ANY_ID, INITIAL_AMOUNT, System.currentTimeMillis());

        when(userPointTable.selectById(ANY_ID)).thenReturn(userPoint);

        // when & then
        mockMvc.perform(get("/point/{id}", ANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANY_ID))
                .andExpect(jsonPath("$.point").value(INITIAL_AMOUNT));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔액보다 많은 금액 사용 시 실패합니다.")
    void usePoint_insufficientBalance() throws Exception {
        // given
        userPointTable.insertOrUpdate(ANY_ID, INITIAL_AMOUNT);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", ANY_ID)
                        .content(String.valueOf(6000L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_success() throws Exception {
        // given
        userPointTable.insertOrUpdate(ANY_ID, INITIAL_AMOUNT);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", ANY_ID)
                        .content(String.valueOf(ANY_AMOUNT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANY_ID))
                .andExpect(jsonPath("$.point").value(INITIAL_AMOUNT - ANY_AMOUNT));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 포인트는 총 10,000원을 넘을 수 없습니다.")
    void chargePoint_exceedLimit() throws Exception {
        //given
        userPointTable.insertOrUpdate(ANY_ID, INITIAL_AMOUNT);

        // when & then
        mockMvc.perform(patch("/point/" + ANY_ID + "/charge")
                        .content(String.valueOf(6000L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 충전 테스트")
    void chargePoint_success() throws Exception {
        mockMvc.perform(patch("/point/" + ANY_ID +"/charge")
                        .content(String.valueOf(ANY_AMOUNT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(ANY_AMOUNT));
    }

    @Test
    @DisplayName("포인트 내역 조회 실패 - 내역이 존재하지 않는 유저")
    void getPointHistory_emptyHistory() throws Exception {
        //given
        long nonExistentUserId = 9999L;

        //when & then
        mockMvc.perform(get("/point/{id}/histories", nonExistentUserId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("해당 유저의 포인트 내역이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("포인트 내역 조회 성공")
    void getPointHistory_success() throws Exception {
        // given
        long updateTime = System.currentTimeMillis();

        List<PointHistory> histories = List.of(
                new PointHistory(1L, ANY_ID, INITIAL_AMOUNT, TransactionType.CHARGE, updateTime),
                new PointHistory(2L, ANY_ID, ANY_AMOUNT, TransactionType.USE, updateTime + 1000)
        );

        when(pointHistoryTable.selectAllByUserId(ANY_ID)).thenReturn(histories);

        // when & then
        mockMvc.perform(get("/point/{id}/histories", ANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(ANY_ID))
                .andExpect(jsonPath("$[0].amount").value(INITIAL_AMOUNT))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].userId").value(ANY_ID))
                .andExpect(jsonPath("$[1].amount").value(ANY_AMOUNT))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }
}
