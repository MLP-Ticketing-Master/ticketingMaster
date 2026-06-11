package com.ticketmaster.backend.domain.seat.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.global.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 좌석 조회 캐시 통합 테스트
 *
 * CacheConfig + SeatService 만 띄운 최소 컨텍스트 — DB 없이 캐시 동작만 검증
 * Repository 는 목으로 대체, "같은 키 재호출 시 실제 조회가 1번만 발생"으로 캐시 적중 확인
 */
@SpringBootTest(classes = {CacheConfig.class, SeatService.class})
@DisplayName("좌석 조회 캐시 통합 테스트")
class SeatServiceCacheIT {

    @Autowired
    private SeatService seatService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private SeatRepository seatRepository;

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private SeatGradeRepository seatGradeRepository;

    @MockitoBean
    private MatchRepository matchRepository;

    @BeforeEach
    void clearCaches() {
        // 컨텍스트가 테스트 간 공유되므로 캐시를 비워 격리 (목 호출 기록은 @MockitoBean 이 매 테스트 리셋)
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("findSectionsByMatch 2번 호출 → DB 조회 1번 (캐시 적중)")
    void 구역목록_캐시적중() {
        // given
        Long matchId = 1L;
        Match match = mock(Match.class, RETURNS_DEEP_STUBS);
        given(match.getEvent().getId()).willReturn(100L);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(100L)).willReturn(List.of());
        given(seatGradeRepository.findAllByEventIdOrderByPriceDesc(100L)).willReturn(List.of());
        given(seatRepository.findIdAndGroupingByMatchId(matchId)).willReturn(List.of());

        // when — 같은 matchId 로 두 번 호출
        seatService.findSectionsByMatch(matchId);
        seatService.findSectionsByMatch(matchId);

        // then — 두 번째는 캐시에서 반환 → 실제 DB 조회는 1번만
        verify(seatRepository, times(1)).findIdAndGroupingByMatchId(matchId);
        verify(matchRepository, times(1)).findById(matchId);
    }

    @Test
    @DisplayName("findSeatsBySection 2번 호출 → DB 조회 1번 (캐시 적중)")
    void 구역좌석_캐시적중() {
        // given
        Long matchId = 1L;
        Long sectionId = 10L;
        Section section = mock(Section.class);
        given(section.getName()).willReturn("A구역");
        given(matchRepository.existsById(matchId)).willReturn(true);
        given(sectionRepository.findById(sectionId)).willReturn(Optional.of(section));
        given(seatRepository.findBySectionAndMatch(matchId, sectionId)).willReturn(List.of());

        // when — 같은 (matchId, sectionId) 로 두 번
        seatService.findSeatsBySection(matchId, sectionId);
        seatService.findSeatsBySection(matchId, sectionId);

        // then — 실제 좌석 조회는 1번만
        verify(seatRepository, times(1)).findBySectionAndMatch(matchId, sectionId);
    }

    @Test
    @DisplayName("findSeatsBySection — sectionId 다르면 키가 달라 각각 DB 조회 (키 분리)")
    void 구역좌석_키별_분리() {
        // given
        Long matchId = 1L;
        Section section = mock(Section.class);
        given(section.getName()).willReturn("구역");
        given(matchRepository.existsById(matchId)).willReturn(true);
        given(sectionRepository.findById(anyLong())).willReturn(Optional.of(section));
        given(seatRepository.findBySectionAndMatch(eq(matchId), anyLong())).willReturn(List.of());

        // when — 서로 다른 구역 2개 + 첫 구역 재호출
        seatService.findSeatsBySection(matchId, 10L);
        seatService.findSeatsBySection(matchId, 20L);
        seatService.findSeatsBySection(matchId, 10L);   // 캐시 적중

        // then — 키(matchId:sectionId)가 달라 10/20 각각 1번씩, 재호출은 캐시
        verify(seatRepository, times(1)).findBySectionAndMatch(matchId, 10L);
        verify(seatRepository, times(1)).findBySectionAndMatch(matchId, 20L);
    }

    @Test
    @DisplayName("TTL(2초) 경과 후 재호출 → 캐시 만료로 DB 다시 조회 (stale 방지)")
    void TTL_만료_후_재조회() throws InterruptedException {
        // given
        Long matchId = 1L;
        Match match = mock(Match.class, RETURNS_DEEP_STUBS);
        given(match.getEvent().getId()).willReturn(100L);
        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(100L)).willReturn(List.of());
        given(seatGradeRepository.findAllByEventIdOrderByPriceDesc(100L)).willReturn(List.of());
        given(seatRepository.findIdAndGroupingByMatchId(matchId)).willReturn(List.of());

        // when — 호출 → TTL(2초) 초과 대기 → 재호출
        seatService.findSectionsByMatch(matchId);
        Thread.sleep(2_500);   // expireAfterWrite 2초 경과
        seatService.findSectionsByMatch(matchId);

        // then — 캐시가 만료돼 DB를 다시 침 → 총 2번
        verify(seatRepository, times(2)).findIdAndGroupingByMatchId(matchId);
    }
}
