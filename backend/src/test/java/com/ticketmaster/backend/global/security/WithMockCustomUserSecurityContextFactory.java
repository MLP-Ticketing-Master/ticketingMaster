package com.ticketmaster.backend.global.security;

import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
	@Override
	public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		// 인성님의 엔티티 구조에 맞게 가짜 User 및 CustomUserDetails 생성
		User user = User.create(customUser.email(), "password", customUser.nickname(), "01012345678");
		CustomUserDetails principal = new CustomUserDetails(user);

		Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
		context.setAuthentication(auth);
		return context;
	}
}