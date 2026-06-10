package com.ticketmaster.backend.domain.user;

import com.ticketmaster.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.ticketmaster.backend.domain.user.dto.request.UpdateUserRequest;
import com.ticketmaster.backend.domain.user.dto.response.UserResponse;
import com.ticketmaster.backend.domain.user.service.UserService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 API", description = "내 정보 조회 / 수정 / 비밀번호 변경 / 회원 탈퇴")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보 반환")
	@GetMapping("/me")
	public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
		// userDetails에서 이메일을 추출하여 서비스에 전달
		UserResponse response = userService.getMyInfo(userDetails.getUsername());
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "내 정보 수정", description = "닉네임 / 전화번호 등 프로필 정보 수정")
	@PatchMapping("/me")
	public ResponseEntity<UserResponse> updateMyInfo(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UpdateUserRequest request
		) {
		UserResponse response = userService.updateMyInfo(userDetails.getUsername(), request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경")
	@PatchMapping("/me/password")
	public ResponseEntity<Void> updatePassword(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UpdatePasswordRequest request
		) {
		userService.updatePassword(userDetails.getUsername(), request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원 탈퇴", description = "로그인한 사용자 계정 소프트 삭제")
	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
		userService.withdraw(userDetails.getUsername());
		return ResponseEntity.noContent().build();
	}
}
