package com.ticketmaster.backend.domain.match.service;

import com.ticketmaster.backend.domain.match.dto.MatchBookingGate;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조회 전용 매치 서비스
 * 예매 게이트는 변동이 거의 없어 캐시 적중률이 매우 높음 - 스파이크 때 DB를 한 번만 침
 */
@Service
@RequiredArgsConstructor
public class MatchQueryService {

    private final MatchRepository matchRepository;

    /**
     * 예매 게이트 조회 (캐시) - 키는 matchId
     * 캐시 미스(첫 호출)일 때만 DB, 이후 30초간 메모리에서 응답
     * 존재하지 않는 매치는 예외라 캐시에 안 담김
     */
    @Cacheable(cacheNames = "match_booking_gate", key = "#matchId")
    @Transactional(readOnly = true)
    public MatchBookingGate getBookingGate(Long matchId) {
        return matchRepository.findBookingGate(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }
}
