package com.ticketmaster.backend.admin.team.service;
import com.ticketmaster.backend.admin.team.dto.request.AdminTeamCreateRequest;
import com.ticketmaster.backend.admin.team.dto.request.AdminTeamUpdateRequest;
import com.ticketmaster.backend.admin.team.dto.response.AdminTeamResponse;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.AdminTeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminTeamServiceTest {

    @InjectMocks
    private AdminTeamService service;

    @Mock
    private AdminTeamRepository teamRepository;

    private static final Long TEAM_ID = 1L;

    private Team t1Team;          // 활성 (T1, LOL)
    private Team genGTeam;        // 활성, 다른 코드 (Gen.G, LOL)
    private Team deletedT1;       // soft-deleted (T1)


    @BeforeEach
    void setUp() {
        t1Team = Team.builder()
                .name("T1")
                .sportType(SportType.LOL)
                .logoImageUrl("https://example.com/t1.png")
                .build();
        ReflectionTestUtils.setField(t1Team, "id", TEAM_ID);

        genGTeam = Team.builder()
                .name("Gen.G")
                .sportType(SportType.LOL)
                .logoImageUrl("https://example.com/geng.png")
                .build();
        ReflectionTestUtils.setField(genGTeam, "id", 2L);

        deletedT1 = Team.builder()
                .name("T1")
                .sportType(SportType.LOL)
                .logoImageUrl("https://example.com/old.png")
                .build();
        ReflectionTestUtils.setField(deletedT1, "id", 99L);
        deletedT1.softDelete();
    }

    // ─── 전체 조회 ───────────────────────────────────

    @Test
    @DisplayName("팀_목록을_최신순으로_반환_필터없음")
    void 팀_목록조회_전체() {
        // given
        given(teamRepository.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(t1Team, genGTeam));

        // when
        List<AdminTeamResponse> result = service.getTeams(null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("T1");
        assertThat(result.get(1).getName()).isEqualTo("Gen.G");
        verify(teamRepository, never()).findAllBySportTypeOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("종목_필터로_팀_목록_반환")
    void 팀_목록조회_필터() {
        // given
        given(teamRepository.findAllBySportTypeOrderByCreatedAtDesc(SportType.LOL))
                .willReturn(List.of(t1Team));

        // when
        List<AdminTeamResponse> result = service.getTeams(SportType.LOL);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSportType()).isEqualTo(SportType.LOL);
        verify(teamRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    // ─── create ─────────────────────────────────────────────

    @Test
    @DisplayName("신규_팀_등록_성공")
    void 팀_등록_신규() {
        // given
        given(teamRepository.findByNameIncludingDeleted("Hanwha"))
                .willReturn(Optional.empty());
        // save() 호출 시 ID 를 박아서 반환 (JPA 동작 흉내)
        given(teamRepository.save(any(Team.class)))
                .willAnswer(inv -> {
                    Team t = inv.getArgument(0);
                    ReflectionTestUtils.setField(t, "id", 50L);
                    return t;
                });

        // when
        AdminTeamResponse res = service.createTeam(
                createReq("Hanwha", SportType.LOL, "https://example.com/hanwha.png"));

        // then
        assertThat(res.getTeamId()).isEqualTo(50L);
        assertThat(res.getName()).isEqualTo("Hanwha");
        assertThat(res.getSportType()).isEqualTo(SportType.LOL);
        assertThat(res.getLogoImageUrl()).isEqualTo("https://example.com/hanwha.png");
        // save 에 넘긴 entity 검증 — 진짜 toEntity() 가 동작했는지
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Hanwha");
        assertThat(captor.getValue().getSportType()).isEqualTo(SportType.LOL);
    }

    @Test
    @DisplayName("활성_상태의_동일_이름_존재시_DUPLICATE_TEAM_NAME")
    void 팀_등록_활성중복() {
        // given
        given(teamRepository.findByNameIncludingDeleted("T1"))
                .willReturn(Optional.of(t1Team));   // 활성 상태

        // when & then
        assertThatThrownBy(() -> service.createTeam(
                createReq("T1", SportType.LOL, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_TEAM_NAME);
        verify(teamRepository, never()).save(any());
        assertThat(t1Team.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된_동일_이름_존재시_복구하고_종목_로고_갱신")
    void 팀_등록_복구() {
        // given
        given(teamRepository.findByNameIncludingDeleted("T1"))
                .willReturn(Optional.of(deletedT1));

        // when
        AdminTeamResponse res = service.createTeam(
                createReq("T1", SportType.VALORANT, "https://new.com/logo.png"));

        // then — 진짜 엔티티 상태 검증: restore() 와 update() 의 실제 동작
        assertThat(deletedT1.isDeleted()).isFalse();
        assertThat(deletedT1.getSportType()).isEqualTo(SportType.VALORANT);
        assertThat(deletedT1.getLogoImageUrl()).isEqualTo("https://new.com/logo.png");
        assertThat(res.getTeamId()).isEqualTo(99L);
        verify(teamRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────

    @Test
    @DisplayName("팀_부분수정_정상")
    void 팀_수정_성공() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(t1Team));
        given(teamRepository.existsByName("T1 Esports")).willReturn(false);

        // when
        AdminTeamResponse res = service.updateTeam(TEAM_ID,
                updateReq("T1 Esports", null, null));

        // then — 진짜 엔티티 상태: update() 가 실제로 적용됨
        assertThat(t1Team.getName()).isEqualTo("T1 Esports");
        assertThat(t1Team.getSportType()).isEqualTo(SportType.LOL);  // 유지
        assertThat(res.getName()).isEqualTo("T1 Esports");
    }

    @Test
    @DisplayName("팀_부분수정_sportType만_변경시_name_유지")
    void 팀_수정_부분() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(t1Team));

        // when
        service.updateTeam(TEAM_ID, updateReq(null, SportType.VALORANT, null));

        // then — Team.update 의 null-safe 동작 검증
        assertThat(t1Team.getName()).isEqualTo("T1");                    // 유지
        assertThat(t1Team.getSportType()).isEqualTo(SportType.VALORANT); // 변경
        // name 변경 없으므로 중복 체크 호출되지 않아야 함
        verify(teamRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("자기_자신과_동일한_이름은_중복체크_스킵")
    void 팀_수정_같은이름() {
        // given - 이름은 그대로, 종목만 변경
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(t1Team));

        // when
        service.updateTeam(TEAM_ID, updateReq("T1", SportType.VALORANT, null));

        // then — existsByName은 호출되지 않아야 함
        verify(teamRepository, never()).existsByName(any());
        assertThat(t1Team.getSportType()).isEqualTo(SportType.VALORANT);
    }

    @Test
    @DisplayName("다른_팀과_이름_중복시_DUPLICATE_TEAM_NAME")
    void 팀_수정_중복() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(t1Team));
        given(teamRepository.existsByName("Gen.G")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.updateTeam(TEAM_ID,
                updateReq("Gen.G", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_TEAM_NAME);
        // 이름이 변경되지 않아야 함
        assertThat(t1Team.getName()).isEqualTo("T1");
    }

    @Test
    @DisplayName("수정_대상이_없으면_TEAM_NOT_FOUND")
    void 팀_수정_없음() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.updateTeam(TEAM_ID,
                updateReq("T1 Esports", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAM_NOT_FOUND);
    }

    // ─── delete ─────────────────────────────────────────────

    @Test
    @DisplayName("팀_삭제_정상_softDelete")
    void 팀_삭제_성공() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(t1Team));

        // when
        service.deleteTeam(TEAM_ID);

        // then - 진짜 엔티티 상태 : softDelete() 가 deletedAt 채움
        assertThat(t1Team.isDeleted()).isTrue();
        assertThat(t1Team.getDeletedAt()).isNotNull();
        // hard delete는 호출되지 않아야 함
        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제_대상이_없으면_TEAM_NOT_FOUND")
    void 팀_삭제_없음() {
        // given
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.deleteTeam(TEAM_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEAM_NOT_FOUND);
    }

    // ─── 헬퍼 (요청 DTO 만 mock) ──────────────────────────

    private AdminTeamCreateRequest createReq(String name, SportType sportType, String logoImageUrl) {
        AdminTeamCreateRequest req = mock(AdminTeamCreateRequest.class);
        given(req.getName()).willReturn(name);
        given(req.getSportType()).willReturn(sportType);
        given(req.getLogoImageUrl()).willReturn(logoImageUrl);
        // toEntity() 도 mock — 실제 Team.builder() 동작 흉내
        given(req.toEntity()).willAnswer(inv -> Team.builder()
                .name(name)
                .sportType(sportType)
                .logoImageUrl(logoImageUrl)
                .build());
        return req;
    }

    private AdminTeamUpdateRequest updateReq(String name, SportType sportType, String logoImageUrl) {
        AdminTeamUpdateRequest req = mock(AdminTeamUpdateRequest.class);
        given(req.getName()).willReturn(name);
        given(req.getSportType()).willReturn(sportType);
        given(req.getLogoImageUrl()).willReturn(logoImageUrl);
        return req;
    }
}