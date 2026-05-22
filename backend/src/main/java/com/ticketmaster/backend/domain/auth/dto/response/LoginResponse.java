package com.ticketmaster.backend.domain.auth.dto.response;

import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)	// 기본 생성자
@AllArgsConstructor	// 모든 필드 생성자(테스트 및 빌더용)
public class LoginResponse {

	// Bearer 인증 헤더에 부착할 Access Token
	private String accessToken;

	// Access Token 만료 시 재발급용 Refresh Token
	private String refreshToken;

	// 프론트 표시·라우팅용 사용자 식별 정보
	private Long userId;
	private String email;
	private String nickname;
	private String phone;
	private Role role;

	/** 사용자 엔티티와 토큰 묶어 응답 객체 생성용 정적 팩토리 */
	public static LoginResponse of(User user, String accessToken, String refreshToken) {
		return LoginResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.userId(user.getId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phone(user.getPhone())
			.role(user.getRole())
			.build();
	}
}
