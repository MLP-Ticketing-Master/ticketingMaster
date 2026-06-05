package com.ticketmaster.backend.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.units.qual.C;
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

        // 좌석 튜닝 때 캐시 추가 예정

        return manager;
    }
}
