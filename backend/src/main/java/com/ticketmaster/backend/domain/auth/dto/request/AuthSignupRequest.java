package com.ticketmaster.backend.domain.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)	// 기본 생성자
@AllArgsConstructor	// 모든 필드 생성사(테스트 및 빌더용)
public class AuthSignupRequest {

	@NotBlank(message = "이메일은 필수 입력값입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수 입력값입니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
	message = "비밀번호는 8자 이상 20자 이하이며, 영문+숫자+특수문자를 포함해야 합니다.")
	private String password;

	@NotBlank(message = "닉네임은 필수 입력값입니다.")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
	private String nickname;

	@Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 이어야 합니다.")
	private String phone;
}
