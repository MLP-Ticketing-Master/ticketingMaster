package com.ticketmaster.backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.queue.util.QueueTokenValidator;
import com.ticketmaster.backend.global.security.queue.QueueAccessFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * QueueAccessFilter 를 좌석 API URL 패턴에만 적용하도록 등록
 *
 * URL 패턴은 서블릿 와일드카드 한계로 /matches/* 까지만 잡고,
 * /matches/{id}/seats/... 정밀 매칭은 Filter 내부 정규식이 담당
 *
 * 순서는 LOWEST_PRECEDENCE — Spring Security 필터 체인 (order=-100) 이후 실행
 * JWT 인증 실패는 큐 검증보다 먼저 발생하도록 의도
 */
@Configuration
public class QueueFilterConfig {

    @Bean
    public FilterRegistrationBean<QueueAccessFilter> queueAccessFilter(
            QueueTokenValidator queueTokenValidator,
            ObjectMapper objectMapper) {

        FilterRegistrationBean<QueueAccessFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new QueueAccessFilter(queueTokenValidator, objectMapper));
        bean.addUrlPatterns("/matches/*");
        bean.setOrder(Ordered.LOWEST_PRECEDENCE);
        return bean;
    }
}
