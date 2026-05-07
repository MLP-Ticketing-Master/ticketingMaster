package com.ticketmaster.backend.admin.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.event.controller.AdminEventController;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.admin.event.service.AdminEventService;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class) // 컨트롤러 테스트용 어노테이션
@Import(SecurityConfig.class)
public class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청(GET, POST 등)을 보내주는 객체

    @Autowired
    private ObjectMapper objectMapper; // 객체를 JSON으로 변환해주는 도구

    @MockitoBean // 스프링 컨테이너에 가짜(Mock) 서비스를 띄워줍니다.
    private AdminEventService adminEventService;

    @Test
    @WithMockUser(roles = "ADMIN") // ADMIN 권한의 가짜 유저로 로그인한 상태를 가정
    @DisplayName("TC-01: ADMIN이 정상적인 데이터를 보내면 대회 생성이 성공하고 201 응답을 반환한다.")
    void 이벤트_등록_201() throws Exception {
        // Given: DTO의 필수(@NotNull, @NotBlank) 필드를 모두 채워줍니다.
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // When & Then: /admin/events 로 POST 요청을 보냅니다.
        mockMvc.perform(post("/admin/events")
                        .with(csrf()) // 스프링 시큐리티 설정에 따라 POST 요청 시 CSRF 토큰이 필요할 수 있습니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // 객체를 JSON 문자열로 변환
                .andDo(print())
                .andExpect(status().isCreated()); // 201 상태 코드 검증
    }

    @Test
    @WithMockUser(roles = "USER") // 일반 유저 권한 가정
    @DisplayName("TC-02: USER 권한 접근 → 403 FORBIDDEN")
    void 이벤트_등록_접근권한_403() throws Exception {
        // Given
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // When & Then
        mockMvc.perform(post("/admin/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 👈 201(isCreated)에서 403(isForbidden)으로 변경!
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-09: 필수 필드 누락 생성 -> 400 Bad Request 응답")
    void 이벤트_등록_필수필드누락_400() throws Exception {
        // Given (타이틀이 누락된 비정상적인 요청 생성)
        AdminEventCreateRequest request = AdminEventCreateRequest.builder()
                .title(null) // @NotBlank 위반!
                .sportType(SportType.LOL)
                .place("LoL Park")
                .startDate(LocalDate.of(2026, 4, 24))
                .endDate(LocalDate.of(2026, 4, 26))
                .bookingOpenAt(LocalDateTime.of(2026, 4, 20, 20, 0))
                .bookingCloseAt(LocalDateTime.of(2026, 4, 26, 22, 0))
                .maxTicketsPerUser(4)
                .cancelFee(1000)
                .build();

        // When & Then
        mockMvc.perform(post("/admin/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 응답 상태 코드가 400(Bad Request)인지 확인!
    }

    // 이벤트 목록 조회 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이벤트 목록 조회 성공(200 반환) - 정상 케이스")
    void 이벤트_목록조회_200() throws Exception {
        // Given: 가짜 서비스가 반환할 가짜 Page 객체 준비
        // (실제 AdminEventListResponse 객체 필드에 맞게 값을 채워주세요)
        List<AdminEventListResponse> fakeList = List.of(
                AdminEventListResponse.builder().eventId(1L).title("2026 LCK 스프링 결승").sportType(SportType.LOL).place("LOL Park").startDate(LocalDate.of(2026, 3, 24)).endDate(LocalDate.of(2026, 5, 11)).status(EventStatus.OPEN).build(),
                AdminEventListResponse.builder().eventId(2L).title("2026 리그오브레전드 월드 챔피언십").sportType(SportType.LOL).place("LOL Park").startDate(LocalDate.of(2026, 5, 24)).endDate(LocalDate.of(2026, 6, 15)).status(EventStatus.OPEN).build(),
                AdminEventListResponse.builder().eventId(2L).title("발로란트 챔피언스 투어 코리아").sportType(SportType.VALORANT).place("코엑스 컨벤션홀").startDate(LocalDate.of(2026, 4, 25)).endDate(LocalDate.of(2026, 5, 15)).status(EventStatus.OPEN).build()
        );
        Page<AdminEventListResponse> fakePage = new PageImpl<>(fakeList);

        // "서비스의 getEventList가 호출되면 가짜 페이지를 반환해라!"
        given(adminEventService.getEventList(any(Pageable.class))).willReturn(fakePage);

        // When & Then: GET 요청을 보내고 검증
        mockMvc.perform(get("/admin/events")
                        // GET 요청이므로 파라미터로 page 정보를 전달 (예: ?page=0&size=10)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 결과를 콘솔에 출력해서 눈으로 확인
                .andExpect(status().isOk()) // 200 OK 응답 확인 (작성하신 isBadRequest() 수정)
                .andExpect(jsonPath("$.content").isArray()) // Page 객체 내부의 리스트인 content가 배열인지 확인
                .andExpect(jsonPath("$.content[0].title").value("2026 LCK 스프링 결승")); // 첫 번째 아이템 검증
    }

    // 이벤트 목록 조회 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 목록 조회 실패 - 접근 권한 에러")
    void 이벤트_목록조회_접근권한에러() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 Forbidden 에러 발생 확인
    }

    // 단일 이벤트 상세 조회 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이벤트 상세 조회 성공 - 정상 케이스")
    void 이벤트_상세조회_정상() throws Exception {
        // Given: 조회할 이벤트 ID와 가짜 서비스가 반환할 응답 객체(DTO) 준비
        Long targetEventId = 1L;

        AdminEventDetailResponse fakeResponse = AdminEventDetailResponse.builder()
                .eventId(1L)
                .title("2026 LCK 스프링 결승")
                .sportType(SportType.LOL)
                .place("LOL Park")
                .thumbnailUrl("https://image.ticketmaster.com/lck-2026-thumb.jpg") // 추가
                .detailImageUrl("https://image.ticketmaster.com/lck-2026-detail.jpg") // 추가
                .description("2026 LCK 스프링 결승전 티켓팅입니다. 많은 관심 부탁드립니다.") // 추가
                .startDate(LocalDate.of(2026, 3, 24))
                .endDate(LocalDate.of(2026, 5, 11))
                .matchDurationText("4시간")
                .ageRating("전체관람가")
                .bookingOpenAt(LocalDateTime.of(2026, 3, 10, 20, 0)) // 추가
                .bookingCloseAt(LocalDateTime.of(2026, 3, 23, 23, 59)) // 추가
                .bookingNotice("예매는 1인당 최대 4매까지 가능하며, 취소 기한을 꼭 확인해주세요.") // 추가
                .maxTicketsPerUser(4)
                .cancelAvailableUntil(LocalDateTime.of(2026, 3, 23, 23, 59)) // 추가
                .cancelFee(1000) // 추가
                .status(EventStatus.OPEN)
                .build();

        // "서비스의 getEventDetail(1L)이 호출되면 우리가 만든 fakeResponse를 반환해라!"
        given(adminEventService.getEventDetail(targetEventId)).willReturn(fakeResponse);

        // When & Then: GET 요청을 보내고 검증
        mockMvc.perform(get("/admin/events/{eventId}", targetEventId) // 🌟 핵심: URL에 변수 넣기
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 결과를 콘솔에 출력
                .andExpect(status().isOk()) // 200 OK 응답 확인
                .andExpect(jsonPath("$.eventId").value(targetEventId)) // JSON 바디의 eventId 검증
                .andExpect(jsonPath("$.title").value("2026 LCK 스프링 결승")) // JSON 바디의 title 검증
                .andExpect(jsonPath("$.place").value("LOL Park"));
    }

    // 단일 이벤트 상세 조회 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER") // 🌟 핵심: ADMIN이 아닌 USER 권한으로 세팅!
    @DisplayName("이벤트 상세 조회 실패 - 접근 권한 에러")
    void 이벤트_상세조회_접근권한에러() throws Exception {
        // Given: 조회하려는 임의의 이벤트 ID
        Long targetEventId = 1L;

        // (주의: 시큐리티 필터에서 먼저 차단당하기 때문에,
        // adminEventService.getEventDetail(...) 동작을 조작하는 given() 코드는 쓸 필요가 없습니다!)

        // When & Then: GET 요청을 보내고 403(Forbidden) 에러가 떨어지는지 검증
        mockMvc.perform(get("/admin/events/{eventId}", targetEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 상태 코드 확인
    }

    // 이벤트 수정 요청 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이벤트 수정 요청 성공 - 일부 필드만 변경")
    void 이벤트_수정요청_정상() throws Exception {
        // Given: 수정할 대상 ID와 "일부" 변경할 데이터 준비
        Long targetEventId = 1L;

        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .place("KINTEX 제1전시장") // 기존 LoL Park에서 변경
                .status(EventStatus.OPEN) // 상태도 변경
                // 나머지는 모두 안보냄 (null)
                .build();

        // 서비스가 반환할 가짜 수정 완료 응답 준비
        // (원래 데이터에서 place와 status만 바뀐 상태를 가정)
        AdminEventResponse fakeUpdatedResponse = AdminEventResponse.builder()
                .eventId(targetEventId)
                .title("2026 LCK 스프링 결승") // 원래 있던 값
                .place("KINTEX 제1전시장") // 🌟 변경된 값
                .status(EventStatus.OPEN) // 🌟 변경된 값
                .build();

        // "서비스의 updateEvent가 1L과 요청 데이터를 받으면 수정된 가짜 응답을 반환해라!"
        // eq(targetEventId)를 써서 반드시 1L이 넘어가야 동작하도록 엄격하게 설정
        given(adminEventService.updateEvent(eq(targetEventId), any(AdminEventUpdateRequest.class)))
                .willReturn(fakeUpdatedResponse);

        // When & Then: PATCH 요청 보내기
        mockMvc.perform(patch("/admin/events/{eventId}", targetEventId) // 🌟 post나 get이 아닌 patch!
                        .with(csrf()) // 상태를 변경하는 요청이므로 csrf 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))) // 부분 데이터만 담긴 JSON
                .andDo(print())
                .andExpect(status().isOk()) // 200 OK 응답
                .andExpect(jsonPath("$.eventId").value(targetEventId))
                .andExpect(jsonPath("$.place").value("KINTEX 제1전시장")) // 수정 요청한 대로 잘 바뀌었는지 검증
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // 이벤트 수정 요청 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER") // 🌟 핵심: ADMIN이 아닌 일반 USER 권한으로 접근 시도!
    @DisplayName("이벤트 수정 요청 실패 - 접근 권한 에러")
    void 이벤트_수정요청_접근권한에러() throws Exception {
        // Given: 타겟 ID와 대충 만든(?) 수정 요청 데이터
        Long targetEventId = 1L;

        AdminEventUpdateRequest updateRequest = AdminEventUpdateRequest.builder()
                .place("KINTEX 제1전시장") // 값이 뭐든 상관없습니다. 어차피 문앞에서 쫓겨납니다.
                .build();

        // (마찬가지로 권한 필터에서 튕기기 때문에 adminEventService의 given() 조작은 필요 없습니다!)

        // When & Then: PATCH 요청을 보내고 403 상태 코드가 나오는지 확인
        mockMvc.perform(patch("/admin/events/{eventId}", targetEventId)
                        .with(csrf()) // POST, PATCH, DELETE 등 데이터를 조작하는 요청엔 csrf 토큰이 필요합니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 Forbidden 검증
    }

    // 이벤트 소프트 삭제 - 정상
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("이벤트 소프트 삭제 성공 - 정상 케이스")
    void 이벤트_소프트삭제_정상() throws Exception {
        // Given: 삭제할 이벤트 ID
        Long targetEventId = 1L;

        // 🌟 핵심: 서비스의 deleteEvent는 반환값이 없는 void 메서드입니다.
        // 이럴 때는 given() 대신 willDoNothing()을 사용합니다!
        willDoNothing().given(adminEventService).deleteEvent(targetEventId);

        // When & Then: DELETE 요청 보내기
        mockMvc.perform(delete("/admin/events/{eventId}", targetEventId)
                        .with(csrf()) // 상태를 변경하는 요청이므로 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent()); // 🌟 204 No Content 응답 확인

        // 추가 검증: 서비스의 deleteEvent 메서드가 정확히 1번 호출되었는지 확인
        verify(adminEventService).deleteEvent(targetEventId);
    }

    // 이벤트 소프트 삭제 - 접근 권한 에러
    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("이벤트 소프트 삭제 실패 - 접근 권한 에러")
    void 이벤트_소프트삭제_접근권한에러() throws Exception {
        // Given: 삭제 시도할 이벤트 ID
        Long targetEventId = 1L;

        // When & Then: 일반 유저(USER)가 DELETE 요청 보내기
        mockMvc.perform(delete("/admin/events/{eventId}", targetEventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // 403 Forbidden 으로 튕겨나는지 확인
    }
}
