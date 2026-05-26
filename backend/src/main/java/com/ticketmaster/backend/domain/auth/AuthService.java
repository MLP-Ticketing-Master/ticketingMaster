package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.PasswordConfirmRequest;
import com.ticketmaster.backend.domain.auth.dto.response.LoginResponse;
import com.ticketmaster.backend.domain.auth.dto.response.TokenResponse;
import com.ticketmaster.backend.domain.auth.dto.request.LoginRequest;
import com.ticketmaster.backend.domain.auth.dto.request.AuthSignupRequest;
import com.ticketmaster.backend.domain.auth.dto.response.AuthSignupResponse;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import com.ticketmaster.backend.global.util.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
	private final MailService mailService;

	public LoginResponse login(LoginRequest request) {
		// 1. 이메일로 사용자 조회
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		// 2. 탈퇴한 회원 여부 확인 (deletedAt이 null이 아니면 소프트 딜리트된 유저)
		if (user.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_USER);
		}

		// 3. 비밀번호 일치 여부 확인
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		// 4. Access/Refresh 토큰 생성
		String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

		// 5. Redis에 Refresh Token 저장 (Key: "RT:" + 이메일, Value: 토큰값)
		// 로그인 시마다 기존 토큰을 덮어씌워 1인 1세션을 유지하거나 보안강화 가능
		redisTemplate.opsForValue().set(
				"RT:" + user.getEmail(),
				refreshToken,
				14,	// 14일간 유효(JwtTokenProvider 설정과 맞춤)
				TimeUnit.DAYS
		);

		// 6. 토큰 + 사용자 정보 묶어 응답 (프론트 표시·라우팅용)
		return LoginResponse.of(user, accessToken, refreshToken);
	}

	@Transactional
	public void logout(String email) {
		// Redis에서 해당 이메일을 키로 가진 REFRESH TOKEN 삭제
		String redisKey = "RT:" + email;
		if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
			redisTemplate.delete(redisKey);
		} else {
			// 이미 로그아웃 되었거나 토큰이 없는 경우 예외 처리
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

	@Transactional
	public AuthSignupResponse signup(AuthSignupRequest request) {
		// 이메일 중복 검증
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		// 비밀번호 BCrypt 암호화 및 엔티티 생성
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		User user = User.create(
				request.getEmail(),
				encodedPassword,
				request.getNickname(),
				request.getPhone()
		);

		// DB 저장
		User savedUser = userRepository.save(user);
		return AuthSignupResponse.from(savedUser);
	}

	@Transactional
	public TokenResponse refresh(String refreshToken) {
		// 1. 토큰 자체의 유효성 검사 (만료 여부 등)
		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
		}

		// 2. 토큰에서 이메일 추출
		String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);

		// 3. Redis에서 해당 이메일의 Refresh Token 가져오기
		String savedRefreshToken = redisTemplate.opsForValue().get("RT:" + email);

		// 4. Redis에 토큰이 없거나 전달받은 토큰과 일치하지 않으면 예외 발생
		if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		// 5. 새로운 Access Token 생성 (Refresh Token은 그대로 유지하거나 같이 갱신 가능)
		// 보안 유지를 위해 유저 정보를 다시 조회하여 최신 Role 반영
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 탈퇴한 회원의 경우 토큰 갱신 차단
		if (user.isDeleted()) {
			throw new BusinessException(ErrorCode.DELETED_USER);
		}

		String newAccessToken= jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());

		// 만약 Refresh Token Rotation(RTR)을 적용한다면 여기서 Refresh Token도 새로 발급하고 Redis를 업데이트
		// 여기서는 Access Token만 새로 내려주는 방식으로 하겠습니다요.
		return new TokenResponse(newAccessToken, refreshToken, user.getRole().name());
	}

	@Transactional
	public void requestPasswordReset(String email) {
		userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String resetToken = UUID.randomUUID().toString();

		// Redis에 저장 (Key: "RESET:토큰값", Value: 이메일)
		redisTemplate.opsForValue().set(
				"RESET:" + resetToken,
				email,
				30, TimeUnit.MINUTES
		);

		mailService.sendResetLink(email, resetToken);
	}

	@Transactional
	public void confirmPasswordReset(PasswordConfirmRequest request) {
		String redisKey = "RESET:" + request.token();
		String email = redisTemplate.opsForValue().get(redisKey);

		if (email == null) {
			throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 비밀번호 암호화 후 업데이트
		user.changePassword(passwordEncoder.encode(request.newPassword()));

		// 사용한 토큰 삭제 및 보안을 위해 기존 모든 리프레시 토큰 삭제(모든 기기 로그아웃)
		redisTemplate.delete(redisKey);
		redisTemplate.delete("RT:" + email);
	}
}