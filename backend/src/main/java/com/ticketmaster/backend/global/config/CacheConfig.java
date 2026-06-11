package com.ticketmaster.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 로컬 캐시 설정
 * 캐시별 TTL/용량을 따로 등록 - 게이트는 길게, 좌석은 짧게(잔여 변동 빠름)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // 기본 스펙 - 등록 안 한 캐시에 적용
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(10000));

        // 예매 게이트 - match/event 정보는 거의 안 변함 -> 30초
        manager.registerCustomCache("match_booking_gate",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(30))
                        .maximumSize(1000)
                        .build());

        // 좌석 캐시 - 잔여가 빠르게 변하므로 TTL 짧게 (2초)
        manager.registerCustomCache("seat_sections",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(2))    // 구역/잔여 — 2초
                        .maximumSize(1000)
                        .build());
        manager.registerCustomCache("seat_section_details",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(2))    // 구역 내 좌석 — 2초
                        .maximumSize(5000)
                        .build());

        return manager;
    }
}
