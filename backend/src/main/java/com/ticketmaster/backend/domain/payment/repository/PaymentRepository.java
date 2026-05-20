package com.ticketmaster.backend.domain.payment.repository;

import com.ticketmaster.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 멱등성 검증용 — 같은 paymentKey 재호출 시 기존 Payment 반환
     */
    Optional<Payment> findByPaymentKey(String paymentKey);

    /**
     * 본인 결제 조회 (결제 상세 조회용)
     */
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.booking b
            JOIN FETCH b.user
            WHERE p.id = :paymentId
            """)
    Optional<Payment> findByIdWithBooking(@Param("paymentId") Long paymentId);
}
