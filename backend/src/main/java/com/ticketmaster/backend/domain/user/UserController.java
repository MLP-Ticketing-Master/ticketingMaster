package com.ticketmaster.backend.domain.user;

import com.ticketmaster.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.ticketmaster.backend.domain.user.dto.request.UpdateUserRequest;
import com.ticketmaster.backend.domain.user.dto.response.UserResponse;
import com.ticketmaster.backend.domain.user.service.UserService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
		// userDetails에서 이메일을 추출하여 서비스에 전달
		UserResponse response = userService.getMyInfo(userDetails.getUsername());
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/me")
	public ResponseEntity<UserResponse> updateMyInfo(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UpdateUserRequest request
		) {
		UserResponse response = userService.updateMyInfo(userDetails.getUsername(), request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/me/password")
	public ResponseEntity<Void> updatePassword(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UpdatePasswordRequest request
		) {
		userService.updatePassword(userDetails.getUsername(), request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.withdraw(userDetails.getUsername());
		return ResponseEntity.noContent().build();
	}
}
