package com.ticketmaster.backend.domain.payment.toss;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 승인 / 환불 API 호출 클라이언트
 *
 * 인증: Basic Auth (Base64 인코딩된 시크릿 키 + ":")
 * 테스트 키는 .env / application.yaml 의 toss.secret-key 에서 주입
 */
@Slf4j
@Component
public class TossPaymentsClient {

    private static final String CONFIRM_PATH = "/payments/confirm";
    private static final String CANCEL_PATH = "/payments/%s/cancel";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${toss.api-base-url}")
    private String baseUrl;

    @Value("${toss.secret-key}")
    private String secretKey;

    /**
     * 결제 승인 요청 — 토스 위젯에서 받은 paymentKey 로 실제 결제 확정
     * 실패 시 TossApiException throw
     */
    public TossPaymentResponse confirm(String paymentKey, String orderId, int amount) {
        String url = baseUrl + CONFIRM_PATH;
        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        log.info("[Toss] confirm 요청 paymentKey={} orderId={} amount={}", paymentKey, orderId, amount);

        try {
            ResponseEntity<TossPaymentResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, TossPaymentResponse.class);
            TossPaymentResponse res = response.getBody();
            log.info("[Toss] confirm 응답 status={} method={}", res.getStatus(), res.getMethod());
            return res;
        } catch (HttpStatusCodeException e) {
            log.warn("[Toss] confirm 결제 실패 paymentKey={} status={} body={}",
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossApiException("토스 결제 승인 실패", e);
        } catch (Exception e) {
            log.error("[Toss] confirm 호출 실패 paymentKey={}", paymentKey, e);
            throw new TossApiException("토스 API 호출 실패", e);
        }
    }

    /**
     * 결제 취소 / 환불 요청
     * 실패 시 TossApiException throw
     */
    public TossPaymentResponse cancel(String paymentKey, String cancelReason) {
        String url = baseUrl + String.format(CANCEL_PATH, paymentKey);
        Map<String, Object> body = Map.of("cancelReason", cancelReason);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        log.info("[Toss] cancel 요청 paymentKey={} reason={}", paymentKey, cancelReason);

        try {
            ResponseEntity<TossPaymentResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, TossPaymentResponse.class);
            log.info("[Toss] cancel 응답 status={}", response.getBody().getStatus());
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("[Toss] cancel 실패 paymentKey={} status={} body={}",
                    paymentKey, e.getStatusCode(), e.getResponseBodyAsString());
            throw new TossApiException("토스 결제 취소 실패", e);
        } catch (Exception e) {
            log.error("[Toss] cancel 호출 실패 paymentKey={}", paymentKey, e);
            throw new TossApiException("토스 API 호출 실패", e);
        }
    }

    /**
     * Basic Auth 헤더 빌드 — secretKey + ":" 를 Base64 인코딩
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
