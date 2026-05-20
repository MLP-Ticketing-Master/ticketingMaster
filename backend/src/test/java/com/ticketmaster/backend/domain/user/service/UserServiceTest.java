package com.ticketmaster.backend.domain.user.service;

import com.ticketmaster.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.ticketmaster.backend.domain.user.dto.request.UpdateUserRequest;
import com.ticketmaster.backend.domain.user.dto.response.UserResponse;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private StringRedisTemplate redisTemplate;

	@InjectMocks private UserService userService;

	private User user;
	private final String email = "test@ticket.com";

	@BeforeEach
	void setUp() {
		// User.java의 create 메서드 사용
		user = User.create(email, "encoded_pw", "인성", "01012345678");
	}

	@Test
	@DisplayName("TC-01: 내 정보 조회 성공")
	void getMyInfo_Success() {
		given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

		UserResponse response = userService.getMyInfo(email);

		assertThat(response.nickname()).isEqualTo("인성");
		assertThat(response.email()).isEqualTo(email);
	}

	@Test
	@DisplayName("TC-03: 내 정보 수정 성공")
	void updateMyInfo_Success() {
		UpdateUserRequest request = new UpdateUserRequest("새닉네임", "01099998888");
		given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

		UserResponse response = userService.updateMyInfo(email, request);

		assertThat(user.getNickname()).isEqualTo("새닉네임");
		assertThat(response.nickname()).isEqualTo("새닉네임");
	}

	@Test
	@DisplayName("TC-05: 비밀번호 변경 실패 - 현재 비밀번호 불일치")
	void updatePassword_Fail_InvalidCurrent() {
		UpdatePasswordRequest request = new UpdatePasswordRequest("wrong_pw", "new_pw");
		given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrong_pw", user.getPassword())).willReturn(false);

		assertThatThrownBy(() -> userService.updatePassword(email, request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
	}

	@Test
	@DisplayName("TC-07: 회원 탈퇴 성공 - 소프트 삭제 및 Redis 토큰 삭제")
	void withdraw_Success() {
		given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
		given(redisTemplate.hasKey("RT:" + email)).willReturn(true);

		userService.withdraw(email);

		assertThat(user.getDeletedAt()).isNotNull();
		assertThat(user.isDeleted()).isTrue();
		verify(redisTemplate, times(1)).delete("RT:" + email);
	}
}