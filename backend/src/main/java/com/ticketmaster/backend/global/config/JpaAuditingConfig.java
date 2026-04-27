package com.ticketmaster.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정
 * - 이 클래스가 있어야 BaseEntity 의 @CreatedDate / @LastModifiedDate 가 자동 동작함
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
