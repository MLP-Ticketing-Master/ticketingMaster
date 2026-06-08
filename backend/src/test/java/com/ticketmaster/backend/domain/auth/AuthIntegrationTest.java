package com.ticketmaster.backend.domain.auth;

import com.ticketmaster.backend.domain.auth.dto.request.AuthSignupRequest;
import com.ticketmaster.backend.domain.user.entity.*;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class AuthIntegrationTest {

	@Container
	@ServiceConnection
	static final OracleContainer ORACLE = new OracleContainer(
			DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
			.withReuse(true);

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379)
			.withReuse(true);

	@DynamicPropertySource
	static void redisProps(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	@Autowired
	private AuthService authService;
	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("회원가입 통합 테스트: 실제 DB  저장 및 권한 확인")
	void signup_Integration() {
		AuthSignupRequest request = new AuthSignupRequest("integration@test.com", "Auth1234!", "통합테스터", "010-1234-5678");
		authService.signup(request);
		User savedUser = userRepository.findByEmail("integration@test.com")
			.orElseThrow(() -> new AssertionError("사용자가 저장되지 않았습니다."));

		assertThat(savedUser.getNickname()).isEqualTo("통합테스터");
		assertThat(savedUser.getRole()).isEqualTo(Role.USER);
	}
}
