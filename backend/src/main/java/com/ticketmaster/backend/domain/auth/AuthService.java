package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.AuthSignupRequest;
import com.ticketmaster.backend.domain.auth.dto.response.AuthSignupResponse;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ticketmaster.backend.global.exception.DuplicateEmailException;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public AuthSignupResponse signup(AuthSignupRequest request) {
		// 이메일 중복 검증
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException();
		}

		// 비밀번호 BCrypt 암호화 및 엔티티 생성
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		User user = User.create(
			request.getEmail(),
			encodedPassword,
			request.getNickname(),
			request.getPhone()
		);

		// DB 저장
		User savedUser = userRepository.save(user);
		return AuthSignupResponse.from(savedUser);
	}
}
