package com.ticketmaster.backend.domain.payment.service;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토스 승인 실패 시 Payment(FAILED) 기록 전용 컴포넌트
 *
 * 별도 트랜잭션(REQUIRES_NEW)으로 분리한 이유:
 * PaymentService.confirm() 의 @Transactional 안에서 BusinessException(RuntimeException)을 throw 하면
 * 같은 트랜잭션이 전부 rollback 되어 Payment.failed 저장도 같이 사라짐
 * 별도 트랜잭션으로 분리해 호출자 rollback과 무관하게 보존
 */
@Service
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Booking booking, String paymentKey, String orderId,
                              int amount, String failureReason) {
        Payment failed = Payment.failed(booking, paymentKey, orderId, amount, failureReason);
        paymentRepository.save(failed);
    }
}
