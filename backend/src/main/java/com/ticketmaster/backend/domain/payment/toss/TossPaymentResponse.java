package com.ticketmaster.backend.domain.payment.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 토스페이먼츠 API 응답 매핑 DTO
 * 토스가 보내는 필드에서 우리가 쓰는 것만 매핑, 나머지는 ignore
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentResponse {

    private String paymentKey;
    private String orderId;
    private String orderName;
    private String status;          // DONE / CANCELED 등 토스 상태
    private String method;          // 카드 / 간편결제 / 계좌이체 등 한국어
    private int totalAmount;
    // 토스 응답은 ISO-8601 with offset (예: 2026-05-22T20:28:03+09:00) — OffsetDateTime 으로 받아야 파싱 가능
    private OffsetDateTime approvedAt;

    // 부하테스트 mock 응답용 정적 팩토리
    public static TossPaymentResponse mock(String paymentKey, String orderId,
                                           int amount, String status, String method) {
        TossPaymentResponse res = new TossPaymentResponse();
        res.paymentKey = paymentKey;
        res.orderId = orderId;
        res.orderName = "loadtest-order";
        res.status = status;
        res.method = method;
        res.totalAmount = amount;
        res.approvedAt = OffsetDateTime.now();
        return res;
    }
}
