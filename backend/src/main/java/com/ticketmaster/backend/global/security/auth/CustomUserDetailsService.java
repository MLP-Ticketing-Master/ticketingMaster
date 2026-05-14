package com.ticketmaster.backend.global.security.auth;

import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// 1. 사용자 조회
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 2. 소프트 삭제 여부 확인 (deletedAt이 null이 아니면 탈퇴한 유저)
		if (user.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		// 3. UserDetails 객체로 변환 (권한에 ROLE_ 접두사 추가)
		return org.springframework.security.core.userdetails.User.builder()
			.username(user.getEmail())
			.password(user.getPassword())
			.authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
			.build();
	}
}
