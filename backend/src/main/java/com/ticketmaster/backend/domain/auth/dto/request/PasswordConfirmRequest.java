package com.ticketmaster.backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 확정 요청")
public record PasswordConfirmRequest(
	@Schema(description = "메일로 받은 재설정 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
	@NotBlank(message = "인증 토큰이 누락되었습니다.")
	String token,
	@Schema(description = "새 비밀번호 (8자 이상)", example = "NewPass1234!")
	@NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
	@Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
	String newPassword
) {}
