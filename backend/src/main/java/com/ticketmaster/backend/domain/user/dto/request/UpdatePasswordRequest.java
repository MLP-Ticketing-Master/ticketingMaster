package com.ticketmaster.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 변경 요청")
public record UpdatePasswordRequest (
	@Schema(description = "현재 비밀번호", example = "Test1234!")
	@NotBlank(message = "현재 비밀번호를 입력해주세요.")
	String currentPassword,

	@Schema(description = "새 비밀번호 (영문+숫자+특수문자 8~20자)", example = "NewPass1234!")
	@NotBlank(message = "새 비밀번호를 입력해주세요.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
	message = "비밀번호는 8~20자이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
	String newPassword
) {}
