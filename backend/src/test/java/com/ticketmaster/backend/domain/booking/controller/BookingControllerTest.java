package com.ticketmaster.backend.domain.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.booking.dto.response.BookingResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingSummaryResponse;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.service.BookingService;
import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean BookingService bookingService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private static final Long USER_ID = 1L;

    // -------------------------------------------------------
    // 예매 생성
    // -------------------------------------------------------

    @Test
    @DisplayName("TC-01: 예매 생성 정상 → 201 + bookingNumber")
    void 예매_생성_정상() throws Exception {
        BookingResponse resp = fakeBookingResponse(10L, "BK20260518ABCD0001", BookingStatus.PENDING);
        given(bookingService.createBooking(eq(USER_ID), any())).willReturn(resp);

        Map<String, Object> body = Map.of("matchId", 5L, "seatIds", List.of(1L, 2L));

        mockMvc.perform(post("/bookings")
                        .with(authentication(userAuth(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(10L))
                .andExpect(jsonPath("$.bookingNumber").value("BK20260518ABCD0001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("TC-04: 타인 점유 좌석 → 409 SEAT_ALREADY_RESERVED")
    void 타인_점유_좌석_예매() throws Exception {
        given(bookingService.createBooking(eq(USER_ID), any()))
                .willThrow(new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED));

        mockMvc.perform(post("/bookings")
                        .with(authentication(userAuth(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("matchId", 5L, "seatIds", List.of(1L)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_ALREADY_RESERVED"));
    }

    @Test
    @DisplayName("TC-05: 만료된 점유 → 410 SEAT_RESERVATION_EXPIRED")
    void 만료된_점유_예매() throws Exception {
        given(bookingService.createBooking(eq(USER_ID), any()))
                .willThrow(new BusinessException(ErrorCode.SEAT_RESERVATION_EXPIRED));

        mockMvc.perform(post("/bookings")
                        .with(authentication(userAuth(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("matchId", 5L, "seatIds", List.of(1L)))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SEAT_RESERVATION_EXPIRED"));
    }

    // -------------------------------------------------------
    // 단건 조회
    // -------------------------------------------------------

    @Test
    @DisplayName("TC-09: 본인 예매 조회 → 200")
    void 본인_예매_조회() throws Exception {
        given(bookingService.getBooking(eq(USER_ID), anyBoolean(), eq(1L)))
                .willReturn(fakeBookingResponse(1L, "BK001", BookingStatus.PENDING));

        mockMvc.perform(get("/bookings/1").with(authentication(userAuth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1L));
    }

    @Test
    @DisplayName("TC-10: 타인 예매 조회 → 403 BOOKING_ACCESS_DENIED")
    void 타인_예매_조회() throws Exception {
        given(bookingService.getBooking(eq(USER_ID), anyBoolean(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.BOOKING_ACCESS_DENIED));

        mockMvc.perform(get("/bookings/99").with(authentication(userAuth(USER_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BOOKING_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("TC-11: 존재하지 않는 bookingId → 404 BOOKING_NOT_FOUND")
    void 없는_예매_조회() throws Exception {
        given(bookingService.getBooking(eq(USER_ID), anyBoolean(), eq(999L)))
                .willThrow(new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        mockMvc.perform(get("/bookings/999").with(authentication(userAuth(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    // -------------------------------------------------------
    // 내 예매 목록
    // -------------------------------------------------------

    @Test
    @DisplayName("TC-12: 내 예매 전체 반환")
    void 내_예매_전체() throws Exception {
        given(bookingService.getMyBookings(eq(USER_ID), isNull(), any()))
                .willReturn(new PageImpl<>(
                        List.of(fakeSummary(1L, BookingStatus.PENDING), fakeSummary(2L, BookingStatus.CONFIRMED)),
                        PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/bookings/me").with(authentication(userAuth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("TC-13: status=CONFIRMED 필터 동작")
    void 상태_필터() throws Exception {
        given(bookingService.getMyBookings(eq(USER_ID), eq(BookingStatus.CONFIRMED), any()))
                .willReturn(new PageImpl<>(List.of(fakeSummary(1L, BookingStatus.CONFIRMED)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/bookings/me?status=CONFIRMED").with(authentication(userAuth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    // -------------------------------------------------------
    // 헬퍼
    // -------------------------------------------------------

    private UsernamePasswordAuthenticationToken userAuth(Long userId) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "email", "u" + userId + "@test.com");
        ReflectionTestUtils.setField(user, "role", Role.USER);
        CustomUserDetails details = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private BookingResponse fakeBookingResponse(Long id, String bookingNumber, BookingStatus status) {
        BookingResponse resp = BeanUtils.instantiateClass(BookingResponse.class);
        ReflectionTestUtils.setField(resp, "bookingId", id);
        ReflectionTestUtils.setField(resp, "bookingNumber", bookingNumber);
        ReflectionTestUtils.setField(resp, "status", status);
        ReflectionTestUtils.setField(resp, "totalPrice", 100_000);
        ReflectionTestUtils.setField(resp, "seats", List.of());
        ReflectionTestUtils.setField(resp, "createdAt", LocalDateTime.now());
        return resp;
    }

    private BookingSummaryResponse fakeSummary(Long id, BookingStatus status) {
        BookingSummaryResponse s = BeanUtils.instantiateClass(BookingSummaryResponse.class);
        ReflectionTestUtils.setField(s, "bookingId", id);
        ReflectionTestUtils.setField(s, "bookingNumber", "BK00" + id);
        ReflectionTestUtils.setField(s, "status", status);
        ReflectionTestUtils.setField(s, "eventTitle", "테스트 대회");
        ReflectionTestUtils.setField(s, "seatCodes", List.of("VIP-A-1"));
        ReflectionTestUtils.setField(s, "totalPrice", 100_000);
        ReflectionTestUtils.setField(s, "createdAt", LocalDateTime.now());
        return s;
    }
}