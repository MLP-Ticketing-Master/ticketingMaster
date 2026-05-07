package com.ticketmaster.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordConfirmRequest(
	@NotBlank(message = "인증 토큰이 누락되었습니다.")
	String token,
	@NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
	String newPassword
) {}
