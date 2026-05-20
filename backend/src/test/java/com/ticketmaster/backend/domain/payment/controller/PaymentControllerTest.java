package com.ticketmaster.backend.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.payment.dto.response.PaymentResponse;
import com.ticketmaster.backend.domain.payment.entity.PaymentMethod;
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import com.ticketmaster.backend.domain.payment.service.PaymentService;
import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@DisplayName("결제 승인 / 조회 컨트롤러 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long USER_ID = 99L;

    @Test
    @DisplayName("POST /payments/confirm 정상 응답 200")
    void 결제_승인_정상() throws Exception {
        // given — paymentService 가 반환할 PaymentResponse 준비
        PaymentResponse res = PaymentResponse.builder()
                .paymentId(99L)
                .bookingId(10L)
                .method(PaymentMethod.CARD)
                .amount(50000)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        given(paymentService.confirm(any(), anyLong())).willReturn(res);

        var body = Map.of(
                "bookingId", 10L,
                "paymentKey", "pk-1",
                "orderId", "order-1",
                "amount", 50000
        );

        // when & then
        mockMvc.perform(post("/payments/confirm")
                        .with(csrf())
                        .with(authentication(userAuth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(99))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ----- 헬퍼 --------------------------------------------------------------

    /** @AuthenticationPrincipal CustomUserDetails 주입용 */
    private Authentication userAuth(Long userId) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "email", "u" + userId + "@test.com");
        ReflectionTestUtils.setField(user, "role", Role.USER);

        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
    }
}