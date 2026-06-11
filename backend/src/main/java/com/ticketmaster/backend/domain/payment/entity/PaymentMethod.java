package com.ticketmaster.backend.domain.payment.entity;

import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;

// 어떤 수단으로 결제했는지
public enum PaymentMethod {
    CARD,           // 신용/체크카드 결제
    EASY_PAY,       // 간편결제 (카카오페이, 네이버페이, 토스페이 등)
    BANK_TRANSFER;  // 계좌이체

    /**
     * 토스 응답의 method 한국어 문자열을 PaymentMethod 로 매핑
     * 토스 응답 예) "카드" / "간편결제" / "계좌이체"
     */
    public static PaymentMethod fromToss(String tossMethod) {
        if (tossMethod == null) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        return switch (tossMethod) {
            case "카드" -> CARD;
            case "간편결제" -> EASY_PAY;
            case "계좌이체" -> BANK_TRANSFER;
            default -> throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        };
    }
}
