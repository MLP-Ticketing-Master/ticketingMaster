package com.ticketmaster.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 대기열 이력 비동기 저장용 실행기
 * 진입 스파이크를 큐로 흡수하고 DB insert는 소수 스레드로 관리
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "queueExecutor")
    public Executor queueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);    // 평상시 스레드
        executor.setMaxPoolSize(8);     // 최대 스레드
        executor.setQueueCapacity(10000);       // 스파이크 흡수 버퍼 - 초과분만 호출 스레드에서
        executor.setThreadNamePrefix("queue-hist-");
        // 큐 + 스레드 다 차서 넘치면 거부(예외) 대신 호출 스레드가 직접 처리
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
