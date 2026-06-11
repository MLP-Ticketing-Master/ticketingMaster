package com.ticketmaster.backend.domain.auth.dto.response;

import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)	// 기본 생성자
@AllArgsConstructor  // 모든 필드 생성사(테스트 및 빌더용)
public class AuthSignupResponse {
	private Long userId;
	private String email;
	private String nickname;
	private String phone;
	private Role role;

	public static AuthSignupResponse from(User user) {
		return AuthSignupResponse.builder()
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phone(user.getPhone())
			.role(user.getRole())
			.build();
	}
}
