package com.ticketmaster.backend.domain.auth.dto.response;

import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthSignupResponse {
	private Long userId;
	private String email;
	private String nickname;
	private Role role;

	public static AuthSignupResponse from(User user) {
		return AuthSignupResponse.builder()
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.role(user.getRole())
			.build();
	}
}
