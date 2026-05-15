package com.ticketmaster.backend.admin.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.match.controller.AdminMatchController;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchCreateRequest;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.admin.match.dto.response.AdminMatchResponse;
import com.ticketmaster.backend.admin.match.service.AdminMatchService;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMatchController.class)
@Import(SecurityConfig.class)
public class AdminMatchControllerTest {
    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청(GET, POST 등)을 보내주는 객체
    @Autowired
    private ObjectMapper objectMapper; // 객체를 JSON으로 변환해주는 도구

    @MockitoBean
    private AdminMatchService adminMatchService;

    // JWT 머지 후 JwtAuthenticationFilter(@Component)가 슬라이스 테스트에 자동 스캔됨
    // → 그 안의 JwtTokenProvider/CustomUserDetailsService를 mock으로 채워야 컨텍스트 로드 성공
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // 전체 매치 목록 조회 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 목록 조회 성공 - 정상 케이스")
    void 매치_목록조회_정상_200() throws Exception {
        // Given: 페이지네이션 정보 준비
        PageRequest pageable = PageRequest.of(0, 10);

        // 알려주신 실제 DTO 구조에 맞춘 가짜(Mock) 응답 데이터
        AdminMatchResponse mockResponse = AdminMatchResponse.builder()
                .id(1L)
                .eventId(100L)
                .roundLabel("결승전")
                .homeTeamId(10L)
                .awayTeamId(20L)
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 21, 0))
                .status(MatchStatus.SCHEDULED)
                .deletedAt(null) // 삭제되지 않은 정상 매치로 가정
                .build();

        // 리스트를 Page 객체로 변환
        Page<AdminMatchResponse> mockPage = new PageImpl<>(List.of(mockResponse), pageable, 1);

        // 가짜 서비스 동작 설정
        given(adminMatchService.getMatchList(isNull(), any(Pageable.class))).willReturn(mockPage);

        // When & Then: GET 요청 전송 및 200 OK와 데이터 검증
        mockMvc.perform(get("/admin/matches")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].eventId").value(100L))
                .andExpect(jsonPath("$.content[0].roundLabel").value("결승전"))
                .andExpect(jsonPath("$.content[0].homeTeamId").value(10L))
                .andExpect(jsonPath("$.content[0].awayTeamId").value(20L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 전체 매치 목록 조회 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("매치 목록 조회 실패 - 접근 권한 에러")
    void 매치_목록조회_접근권한에러_403() throws Exception {
        // Given: 서비스 계층(Mock)의 동작을 정의할 필요가 없습니다. (Security 필터에서 차단됨)

        // When & Then: GET 요청 전송 및 403 Forbidden 검증
        mockMvc.perform(get("/admin/matches")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isForbidden()); // 401(Unauthorized)이 아닌 403(Forbidden) 기대
    }

    // 매치 등록 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 등록 성공 - 정상 케이스")
    void 매치_등록_정상_201() throws Exception {
        // Given: URL Path Variable로 사용할 eventId
        Long eventId = 100L;

        // 1. 요청 데이터(Request Body) 준비
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 19, 0))
                .homeTeamId(10L)
                .awayTeamId(20L)
                .build();

        // 2. 서비스 레이어에서 반환해 줄 가짜(Mock) 응답 데이터 준비
        AdminMatchResponse mockResponse = AdminMatchResponse.builder()
                .id(1L)
                .eventId(eventId)
                .roundLabel("결승전")
                .homeTeamId(10L)
                .awayTeamId(20L)
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 19, 0))
                .status(MatchStatus.SCHEDULED) // 엔티티 Builder에 정의된 기본값(SCHEDULED) 반영
                .deletedAt(null)
                .build();

        // 3. 가짜 서비스 동작 설정
        given(adminMatchService.createMatch(eq(eventId), any(AdminMatchCreateRequest.class)))
                .willReturn(mockResponse);

        // When & Then: POST 요청 전송 및 201 CREATED 상태와 데이터 검증
        mockMvc.perform(post("/admin/events/{eventId}/matches", eventId)
                        .with(csrf()) // POST 요청이므로 CSRF 토큰 주입
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // DTO 객체를 JSON 문자열로 직렬화
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isCreated()) // 201 Created 기대
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.roundLabel").value("결승전"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    // 매치 등록 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("매치 등록 실패 - 접근 권한 에러")
    void 매치_등록_접근권한에러_403() throws Exception {
        // Given: URL Path Variable
        Long eventId = 100L;

        // 비록 Security 필터에서 차단되지만, 올바른 요청 형식을 갖춰 보내는 것이 좋습니다.
        AdminMatchCreateRequest request = AdminMatchCreateRequest.builder()
                .roundLabel("결승전")
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .build();

        // 서비스 Mocking(given)은 Security 단에서 튕기므로 불필요합니다.

        // When & Then: POST 요청 전송 및 403 Forbidden 검증
        mockMvc.perform(post("/admin/events/{eventId}/matches", eventId)
                        .with(csrf()) // ★ 중요: 권한 테스트라도 POST 요청에는 반드시 CSRF 토큰을 넣어야 합니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403(Forbidden) 에러 기대
    }

    // 매치 등록 - 필수 필드 누락
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 등록 실패 - 필수 필드 누락")
    void 매치_등록_필수필드누락_400() throws Exception {
        // Given: URL Path Variable
        Long eventId = 100L;

        // 필수 필드인 roundLabel(@NotBlank)과 matchDate(@NotNull)를 고의로 누락
        AdminMatchCreateRequest invalidRequest = AdminMatchCreateRequest.builder()
                // .roundLabel("결승전") -> 의도적 누락
                // .matchDate(LocalDate.of(2026, 4, 26)) -> 의도적 누락
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .homeTeamId(10L)
                .awayTeamId(20L)
                .build();

        // 입력값 검증(Validation) 단계에서 튕기기 때문에 서비스 Mocking(given)은 불필요합니다.

        // When & Then: POST 요청 전송 및 400 Bad Request 검증
        mockMvc.perform(post("/admin/events/{eventId}/matches", eventId)
                        .with(csrf()) // POST 요청이므로 CSRF 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400(Bad Request) 에러 기대
    }

    // 특정 매치 상세 조회 (수정 페이지) - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 상세 조회 성공 - 정상 케이스")
    void 매치_상세조회_정상_200() throws Exception {
        // Given: 조회할 매치 ID
        Long matchId = 1L;

        // 서비스 레이어에서 반환해 줄 가짜(Mock) 응답 데이터 준비
        AdminMatchResponse mockResponse = AdminMatchResponse.builder()
                .id(matchId)
                .eventId(100L)
                .roundLabel("4강전") // 수정할 매치 데이터라고 가정
                .homeTeamId(10L)
                .awayTeamId(20L)
                .matchDate(LocalDate.of(2026, 5, 10))
                .startAt(LocalDateTime.of(2026, 5, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 5, 10, 20, 0))
                .status(MatchStatus.SCHEDULED)
                .deletedAt(null)
                .build();

        // 가짜 서비스 동작 설정: 해당 matchId가 들어오면 mockResponse를 반환해라!
        given(adminMatchService.getMatchDetail(matchId)).willReturn(mockResponse);

        // When & Then: GET 요청 전송 및 200 OK와 데이터 검증
        mockMvc.perform(get("/admin/matches/{matchId}", matchId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isOk()) // 200 OK 기대
                .andExpect(jsonPath("$.id").value(matchId))
                .andExpect(jsonPath("$.eventId").value(100L))
                .andExpect(jsonPath("$.roundLabel").value("4강전"))
                .andExpect(jsonPath("$.homeTeamId").value(10L))
                .andExpect(jsonPath("$.awayTeamId").value(20L))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    // 특정 매치 상세 조회 (수정 페이지) - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("매치 상세 조회 실패 - 접근 권한 에러")
    void 매치_상세조회_접근권한에러_403() throws Exception {
        // Given: URL Path Variable로 넘길 임의의 매치 ID
        Long matchId = 1L;

        // Security 필터(인가) 단계에서 차단되므로 서비스 레이어(matchService) Mocking은 불필요합니다.

        // When & Then: GET 요청 전송 및 403 Forbidden 검증
        mockMvc.perform(get("/admin/matches/{matchId}", matchId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isForbidden()); // 403 Forbidden 기대
    }

    // 매치 수정 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 수정 성공 - 정상 케이스")
    void 매치_수정_정상_200() throws Exception {
        // Given: 수정할 매치 ID
        Long matchId = 1L;

        // 1. 요청 데이터(Request Body) 준비 - PATCH이므로 수정할 필드만 세팅
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("결승전 (시간변경)") // 라벨 변경
                .status(MatchStatus.LIVE) // 상태를 LIVE로 변경
                // 나머지 필드(matchDate 등)는 null로 두어 수정하지 않음을 의도함
                .build();

        // 2. 서비스 레이어에서 반환해 줄 가짜(Mock) 응답 데이터 준비 (수정된 결과)
        AdminMatchResponse mockResponse = AdminMatchResponse.builder()
                .id(matchId)
                .eventId(100L)
                .roundLabel("결승전 (시간변경)") // 수정 반영
                .homeTeamId(10L)
                .awayTeamId(20L)
                .matchDate(LocalDate.of(2026, 4, 26))
                .startAt(LocalDateTime.of(2026, 4, 26, 17, 0))
                .endAt(LocalDateTime.of(2026, 4, 26, 19, 0))
                .status(MatchStatus.LIVE) // 수정 반영
                .deletedAt(null)
                .build();

        // 3. 가짜 서비스 동작 설정
        given(adminMatchService.updateMatch(eq(matchId), any(AdminMatchUpdateRequest.class)))
                .willReturn(mockResponse);

        // When & Then: PATCH 요청 전송 및 200 OK와 데이터 검증
        mockMvc.perform(patch("/admin/matches/{matchId}", matchId)
                        .with(csrf()) // PATCH 요청이므로 CSRF 토큰 주입 필수!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // DTO를 JSON으로 직렬화
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isOk()) // 200 OK 기대
                .andExpect(jsonPath("$.id").value(matchId))
                .andExpect(jsonPath("$.roundLabel").value("결승전 (시간변경)"))
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    // 매치 수정 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("매치 수정 실패 - 접근 권한 에러")
    void 매치_수정_접근권한에러_403() throws Exception {
        // Given: 수정할 매치 ID
        Long matchId = 1L;

        // Security 단에서 차단되므로 값의 유효성은 크게 중요하지 않지만, 형식은 맞춰서 보냅니다.
        AdminMatchUpdateRequest request = AdminMatchUpdateRequest.builder()
                .roundLabel("권한 없는 수정 시도")
                .build();

        // 서비스 Mocking(given)은 Security 필터에서 튕기므로 불필요합니다.

        // When & Then: PATCH 요청 전송 및 403 Forbidden 검증
        mockMvc.perform(patch("/admin/matches/{matchId}", matchId)
                        .with(csrf()) // ★ 중요: Role 테스트라도 PATCH 요청에는 반드시 CSRF 토큰 삽입!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isForbidden()); // 403(Forbidden) 에러 기대
    }

    // 매치 삭제 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("매치 삭제 성공 - 정상 케이스")
    void 매치_삭제_정상_() throws Exception {
        // Given: 삭제할 매치 ID
        Long matchId = 1L;

        // 서비스의 deleteMatch 메서드는 반환 타입이 void 입니다.
        // void 메서드의 정상 동작을 Mocking 할 때는 given() 대신 willDoNothing()을 사용합니다.
        // (사실 생략해도 Mock 객체의 기본 동작으로 인해 무사히 넘어가지만, 명시적으로 작성하는 것이 가독성에 좋습니다.)
        willDoNothing().given(adminMatchService).deleteMatch(matchId);

        // When & Then: DELETE 요청 전송 및 204 No Content 검증
        mockMvc.perform(delete("/admin/matches/{matchId}", matchId)
                        .with(csrf()) // ★ 중요: 데이터를 변경(삭제)하는 요청이므로 CSRF 토큰 필수!
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isNoContent()); // 204 No Content (ResponseEntity.noContent().build()) 기대
    }

    // 매치 삭제 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("매치 삭제 실패 - 접근 권한 에러")
    void 매치_삭제_접근권한에러_() throws Exception {
        // Given: 삭제를 시도할 매치 ID
        Long matchId = 1L;

        // Security 필터(인가) 단계에서 차단되므로 matchService.deleteMatch() 동작 정의는 불필요합니다.

        // When & Then: DELETE 요청 전송 및 403 Forbidden 검증
        mockMvc.perform(delete("/admin/matches/{matchId}", matchId)
                        .with(csrf()) // ★ 중요: Role 테스트라도 DELETE 요청에는 반드시 CSRF 토큰 삽입!
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isForbidden()); // 403(Forbidden) 에러 기대
    }
}
