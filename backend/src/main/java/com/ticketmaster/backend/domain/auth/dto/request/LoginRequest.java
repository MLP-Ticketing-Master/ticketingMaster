package com.ticketmaster.backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "로그인 요청")
@Getter
@RequiredArgsConstructor
public class LoginRequest {
	@Schema(description = "이메일", example = "user@ticketmaster.com")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	private final String email;

	@Schema(description = "비밀번호", example = "Test1234!")
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	private final String password;
}