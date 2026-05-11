package com.ticketmaster.backend.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-expiration}")
	private long accessExpirationTime;

	@Value("${jwt.refresh-expiration}")
	private long refreshExpirationTime;

	private Key key;

	@PostConstruct
	protected  void init() {
		byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateAccessToken(String email, String role) {
		Claims claims = Jwts.claims().setSubject(email);
		claims.put("role", role);

		Date now = new Date();
		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + accessExpirationTime))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public String generateRefreshToken(String email) {
		Date now = new Date();
		return Jwts.builder()
			.setSubject(email)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + refreshExpirationTime))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.error("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.error("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.error("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.error("JWT 토큰이 비어있습니다.");
		}
		return false;
	}

	public String getUserEmailFromToken(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.getSubject();
	}

	public String getRoleFromToken(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody()
			.get("role", String.class);
	}
}
