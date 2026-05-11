package com.ticketmaster.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 파싱을 위해 기본 생성자 필요
public class RefreshRequest {
	@NotBlank(message = "Refresh Token은 필수입니다.")
	private String refreshToken;
}