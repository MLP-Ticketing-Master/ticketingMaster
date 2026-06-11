package com.ticketmaster.backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "토큰 재발급 요청")
@Getter
@NoArgsConstructor // JSON 파싱을 위해 기본 생성자 필요
public class RefreshRequest {
	@Schema(description = "발급받은 Refresh 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
	@NotBlank(message = "Refresh Token은 필수입니다.")
	private String refreshToken;
}