package com.ticketmaster.backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Schema(description = "회원가입 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)	// 기본 생성자
@AllArgsConstructor	// 모든 필드 생성사(테스트 및 빌더용)
public class AuthSignupRequest {

	@Schema(description = "이메일", example = "user@ticketmaster.com")
	@NotBlank(message = "이메일은 필수 입력값입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	private String email;

	@Schema(description = "비밀번호 (영문+숫자+특수문자 8~20자)", example = "Test1234!")
	@NotBlank(message = "비밀번호는 필수 입력값입니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
	message = "비밀번호는 8자 이상 20자 이하이며, 영문+숫자+특수문자를 포함해야 합니다.")
	private String password;

	@Schema(description = "닉네임 (2~20자)", example = "강이")
	@NotBlank(message = "닉네임은 필수 입력값입니다.")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
	private String nickname;

	@Schema(description = "전화번호 (선택)", example = "010-1234-5678")
	@Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 이어야 합니다.")
	private String phone;
}
