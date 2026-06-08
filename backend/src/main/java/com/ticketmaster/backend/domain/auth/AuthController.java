package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.*;
import com.ticketmaster.backend.domain.auth.dto.response.AuthSignupResponse;
import com.ticketmaster.backend.domain.auth.dto.response.LoginResponse;
import com.ticketmaster.backend.domain.auth.dto.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@GetMapping("/check-email")
	public ResponseEntity<Void> checkEmail(@RequestParam String email) {
		authService.checkEmailDuplicate(email);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/signup")
	public ResponseEntity<AuthSignupResponse> signup(@Valid @RequestBody AuthSignupRequest request) {
		AuthSignupResponse response = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
		LoginResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
		// 현재 인증된 사용자의 이메일을 가져와 로그아웃 처리
		authService.logout(userDetails.getUsername());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		TokenResponse response = authService.refresh(request.getRefreshToken());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/password-reset/request")
	public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequest request) {
		authService.requestPasswordReset(request.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/password-reset/confirm")
	public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordConfirmRequest request) {
		authService.confirmPasswordReset(request);
		return ResponseEntity.ok().build();
	}
}
