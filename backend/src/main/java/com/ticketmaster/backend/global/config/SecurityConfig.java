package com.ticketmaster.backend.global.config;

import com.ticketmaster.backend.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(ex -> ex
				// 1. 인증 실패 처리 (401 Unauthorized)
				.authenticationEntryPoint((req, res, e) -> {
					res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					res.setContentType("application/json;charset=UTF-8");
					res.getWriter().write("{\"code\":\"UNAUTHORIZED\", \"message\":\"로그인이 필요합니다.\"}");
				})
				// 2. 권한 부족 처리 (403 Forbidden)
				.accessDeniedHandler((req, res, e) -> {
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
					res.setContentType("application/json;charset=UTF-8");
					res.getWriter().write("{\"code\":\"FORBIDDEN\", \"message\":\"접근 권한이 없습니다.\"}");
				})
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/auth/logout").authenticated()						// 로그아웃은 인증 필요
				.requestMatchers("/auth/**").permitAll()										// 로그인, 회원가입, 토큰 재발급은 전체 허용
				.requestMatchers("/events/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated()
			).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// 허용할 프론트엔드 주소 (로컬 테스트용 5173 포트)
		configuration.addAllowedOrigin("http://localhost:5173");

		// 허용할 HTTP 메소드
		configuration.addAllowedMethod("*");

		// 허용할 헤더
		configuration.addAllowedHeader("*");

		// 자격 증명 허용 (쿠키나 Authorization 헤더를 주고받을 때 필수)
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
