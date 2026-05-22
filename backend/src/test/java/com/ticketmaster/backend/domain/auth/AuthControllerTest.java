package com.ticketmaster.backend.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.auth.dto.request.AuthSignupRequest;
import com.ticketmaster.backend.domain.auth.dto.request.LoginRequest;
import com.ticketmaster.backend.domain.auth.dto.response.LoginResponse;
import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class  AuthControllerTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@MockitoBean private AuthService authService;

	// JWT 머지 후 JwtAuthenticationFilter(@Component)가 슬라이스 테스트에 자동 스캔됨
	// → 그 안의 JwtTokenProvider/CustomUserDetailsService를 mock으로 채워야 컨텍스트 로드 성공
	@MockitoBean private JwtTokenProvider jwtTokenProvider;
	@MockitoBean private CustomUserDetailsService customUserDetailsService;

	@Test
	@DisplayName("TC-03: 이메일 형식 오류 시 400 에러 반환")
	void signup_InvalidEmail() throws Exception {
		AuthSignupRequest request = new AuthSignupRequest("invalid-email", "Pw123!", "nick", null);

		mockMvc.perform(post("/auth/signup")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("TC-12: 전화번호 형식 오류 시 400 에러 반환")
	void signup_InvalidPhone() throws Exception {
		AuthSignupRequest request = new AuthSignupRequest("test@test.com", "Pw123!", "nick", "01012345678");

		mockMvc.perform(post("/auth/signup")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("TC-LG-01: 로그인 성공 시 200 + 토큰·사용자 정보 반환")
	void login_Success() throws Exception {
		// given
		LoginRequest request = new LoginRequest("test@test.com", "Pw123!");
		LoginResponse mockResponse = LoginResponse.builder()
			.accessToken("at_token")
			.refreshToken("rt_token")
			.userId(1L)
			.email("test@test.com")
			.nickname("nick")
			.phone("010-1234-5678")
			.role(Role.USER)
			.build();
		given(authService.login(any(LoginRequest.class))).willReturn(mockResponse);

		// when
		ResultActions result = mockMvc.perform(post("/auth/login")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)));

		// then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("at_token"))
			.andExpect(jsonPath("$.refreshToken").value("rt_token"))
			.andExpect(jsonPath("$.userId").value(1))
			.andExpect(jsonPath("$.email").value("test@test.com"))
			.andExpect(jsonPath("$.nickname").value("nick"))
			.andExpect(jsonPath("$.phone").value("010-1234-5678"))
			.andExpect(jsonPath("$.role").value("USER"));
	}

}
