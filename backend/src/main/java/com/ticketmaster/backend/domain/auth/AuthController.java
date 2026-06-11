package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.*;
import com.ticketmaster.backend.domain.auth.dto.response.AuthSignupResponse;
import com.ticketmaster.backend.domain.auth.dto.response.LoginResponse;
import com.ticketmaster.backend.domain.auth.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "회원가입 / 로그인 / 토큰 재발급 / 비밀번호 재설정")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 사용 가능 여부 확인 (중복이면 409)")
	@GetMapping("/check-email")
	public ResponseEntity<Void> checkEmail(@Parameter(description = "확인할 이메일") @RequestParam String email) {
		authService.checkEmailDuplicate(email);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원가입", description = "이메일 / 비밀번호 / 닉네임으로 신규 회원 등록")
	@PostMapping("/signup")
	public ResponseEntity<AuthSignupResponse> signup(@Valid @RequestBody AuthSignupRequest request) {
		AuthSignupResponse response = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "로그인", description = "이메일 / 비밀번호 인증 후 Access / Refresh 토큰 발급")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
		LoginResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "로그아웃", description = "인증된 사용자의 Refresh 토큰 폐기")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
		// 현재 인증된 사용자의 이메일을 가져와 로그아웃 처리
		authService.logout(userDetails.getUsername());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "토큰 재발급", description = "유효한 Refresh 토큰으로 새 Access 토큰 발급")
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		TokenResponse response = authService.refresh(request.getRefreshToken());
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "비밀번호 재설정 요청", description = "가입 이메일로 재설정 링크 메일 발송")
	@PostMapping("/password-reset/request")
	public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequest request) {
		authService.requestPasswordReset(request.email());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "비밀번호 재설정 확정", description = "메일로 받은 토큰과 새 비밀번호로 변경 완료")
	@PostMapping("/password-reset/confirm")
	public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordConfirmRequest request) {
		authService.confirmPasswordReset(request);
		return ResponseEntity.ok().build();
	}
}
