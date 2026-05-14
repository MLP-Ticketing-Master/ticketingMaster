package com.ticketmaster.backend.domain.user.service;

import com.ticketmaster.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.ticketmaster.backend.domain.user.dto.request.UpdateUserRequest;
import com.ticketmaster.backend.domain.user.dto.response.UserResponse;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final StringRedisTemplate redisTemplate;

	public UserResponse getMyInfo(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse updateMyInfo(String email, UpdateUserRequest request) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		user.updateProfile(request.nickname(), request.phone());

		return UserResponse.from(user);
	}

	@Transactional
	public void updatePassword(String email, UpdatePasswordRequest request) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 1. 현재 비밀번호 일치 여부 확인
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}

		// 2. 새 비밀번호 암호화 및 저장
		user.changePassword(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional
	public void withdraw(String email) {
		// 1. 사용자 조회
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 2. 이미 탈퇴한 사용자인지 체크
		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		// 3. 소프트 삭제 수행
		user.withdraw();

		// 4. Redis에서 해당 사용자의 Refresh Token 삭제 (로그아웃 처리)
		String redisKey = "RT:" + email;
		if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			redisTemplate.delete(redisKey);
		}

	}
}
