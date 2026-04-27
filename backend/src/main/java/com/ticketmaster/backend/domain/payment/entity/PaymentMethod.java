package com.ticketmaster.backend.domain.payment.entity;

// 어떤 수단으로 결제했는지
public enum PaymentMethod {
    CARD,           // 신용/체크카드 결제
    EASY_PAY,       // 간편결제 (카카오페이, 네이버페이, 토스페이 등)
    BANK_TRANSFER   // 계좌이체 / 무통장입금
}
