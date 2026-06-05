package com.ticketmaster.backend.domain.match.service;

import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.match.dto.MatchBookingGate;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 예매 게이트 조회 서비스 단위 테스트
 *
 * @Cacheable 캐시 적중 동작은 Spring 컨텍스트가 있어야 검증 가능 → 통합 테스트 영역
 * 여기서는 조회 위임 / 미존재 시 예외 변환만 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("예매 게이트 조회 서비스 단위 테스트")
class MatchQueryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchQueryService matchQueryService;

    @Test
    @DisplayName("TC-01: 게이트 존재 → 조회 결과 그대로 반환")
    void 게이트_조회_성공() {
        // given
        MatchBookingGate gate = new MatchBookingGate(
                EventStatus.OPEN, MatchStatus.SCHEDULED,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        given(matchRepository.findBookingGate(1L)).willReturn(Optional.of(gate));

        // when
        MatchBookingGate result = matchQueryService.getBookingGate(1L);

        // then
        assertThat(result).isSameAs(gate);
    }

    @Test
    @DisplayName("TC-02: 게이트 없음(존재하지 않는 매치) → MATCH_NOT_FOUND")
    void 게이트_없음_예외() {
        // given — projection 조회가 빈 Optional 반환
        given(matchRepository.findBookingGate(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> matchQueryService.getBookingGate(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
    }
}
