package com.ticketmaster.backend.domain.user.dto.response;

import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.entity.Role;
import lombok.Builder;

@Builder
public record UserResponse (
	Long userId,
	String email,
	String nickname,
	String phone,
	Role role
) {
	public static UserResponse from(User user) {
		return UserResponse.builder()
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phone(user.getPhone())
			.role(user.getRole())
			.build();
	}
}
