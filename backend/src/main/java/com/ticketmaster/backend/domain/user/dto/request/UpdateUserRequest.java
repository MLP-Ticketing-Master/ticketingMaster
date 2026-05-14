package com.ticketmaster.backend.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest (
	@Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
	String nickname,

	@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)")
	String phone
) {}
