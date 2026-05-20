package com.ticketmaster.backend.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.ticketmaster.backend.domain.user.dto.request.UpdateUserRequest;
import com.ticketmaster.backend.domain.user.service.UserService;
import com.ticketmaster.backend.global.security.WithMockCustomUser;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService; // 추가
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	// 에러 해결의 핵심: 필터에서 사용하는 의존성을 가짜 빈으로 등록
	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@WithMockCustomUser
	@DisplayName("TC-02: 내 정보 조회 API 성공")
	void getMyInfo_ApiSuccess() throws Exception {
		mockMvc.perform(get("/users/me"))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockCustomUser
	@DisplayName("TC-04: 내 정보 수정 API 성공")
	void updateMyInfo_ApiSuccess() throws Exception {
		UpdateUserRequest request = new UpdateUserRequest("새닉네임", "010-1111-2222");

		mockMvc.perform(patch("/users/me")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}

	@Test
	@WithMockCustomUser
	@DisplayName("TC-06: 비밀번호 변경 API 성공")
	void updatePassword_ApiSuccess() throws Exception {
		UpdatePasswordRequest request = new UpdatePasswordRequest("current_pw", "NewPassword123!");

		mockMvc.perform(patch("/users/me/password")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
	}

	@Test
	@WithMockCustomUser
	@DisplayName("TC-08: 회원 탈퇴 API 성공")
	void withdraw_ApiSuccess() throws Exception {
		mockMvc.perform(delete("/users/me")
				.with(csrf()))
			.andExpect(status().isNoContent());
	}
}