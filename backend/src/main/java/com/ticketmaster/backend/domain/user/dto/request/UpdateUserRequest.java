package com.ticketmaster.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "내 정보 수정 요청")
public record UpdateUserRequest (
	@Schema(description = "닉네임 (2~30자)", example = "강이")
	@Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
	String nickname,

	@Schema(description = "전화번호", example = "010-1234-5678")
	@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)")
	String phone
) {}
