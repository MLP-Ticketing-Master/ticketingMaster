package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.AuthSignupRequest;
import com.ticketmaster.backend.domain.auth.dto.response.AuthSignupResponse;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@InjectMocks private AuthService authService;

	@Test
	@DisplayName("TC-01, 11: 성공 케이스 (전화번호 없음)")
	void signup_success() {
		AuthSignupRequest request = new AuthSignupRequest("test@test.com", "Pw123!", "nick", null);

		given(userRepository.existsByEmail(anyString())).willReturn(false);
		given(passwordEncoder.encode(anyString())).willReturn("encoded_pw");
		given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

		AuthSignupResponse response = authService.signup(request);

		assertThat(response.getEmail()).isEqualTo("test@test.com");
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("TC-02: 이메일 중복 시 예외 발생")
	void signup_fail_duplicate() {
		AuthSignupRequest request = new AuthSignupRequest("dup@test.com", "Pw123!", "nick", null);
		given(userRepository.existsByEmail(anyString())).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("TC-09: 비밀번호가 BCrypt로 암호화되어 저장되는지 확인")
	void signup_VerifyEncoding() {
		AuthSignupRequest request = new AuthSignupRequest("test@test.com", "raw_pw", "nick", null);
		given(passwordEncoder.encode("raw_pw")).willReturn("bcrypted_pw");
		given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

		authService.signup(request);

		verify(passwordEncoder).encode("raw_pw");
		verify(userRepository).save(argThat(user -> user.getPassword().equals("bcrypted_pw")));
	}
}
