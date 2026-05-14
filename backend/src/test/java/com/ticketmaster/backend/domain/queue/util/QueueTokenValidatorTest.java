package com.ticketmaster.backend.domain.queue.util;


import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * TK-88 입장 권한 검증 유틸 단위 테스트
 *
 * 좌석 점유 / 결제 같은 보호 대상 API 가 호출하는 검증 메서드
 * Redis hasKey 결과만 가지고 분기 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("입장 권한 검증 유틸 단위 테스트")
class QueueTokenValidatorTest {

    @Mock
    private QueueRedisRepository queueRedis;

    @InjectMocks
    private QueueTokenValidator queueTokenValidator;

    @Test
    @DisplayName("TC-11: 토큰 null/blank → QUEUE_TOKEN_NOT_FOUND")
    void 토큰_누락() {
        // when & then — null
        assertThatThrownBy(() -> queueTokenValidator.validateAllowed(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_NOT_FOUND);

        // when & then — 빈 문자열 (공백만)
        assertThatThrownBy(() -> queueTokenValidator.validateAllowed(1L, "   "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("TC-12: ALLOWED 아님 → QUEUE_NOT_PASSED")
    void 권한_없음() {
        // given — Redis 에 allowed 키 없음 (만료됐거나 위조 토큰)
        given(queueRedis.isAllowed(1L, "tk")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> queueTokenValidator.validateAllowed(1L, "tk"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_NOT_PASSED);
    }

    @Test
    @DisplayName("TC-13: 정상 → 예외 X")
    void 정상() {
        // given — Redis 에 allowed 키 있음
        given(queueRedis.isAllowed(1L, "tk")).willReturn(true);

        // when & then — 통과 (예외 없이 정상 종료)
        assertThatCode(() -> queueTokenValidator.validateAllowed(1L, "tk"))
                .doesNotThrowAnyException();
    }
}