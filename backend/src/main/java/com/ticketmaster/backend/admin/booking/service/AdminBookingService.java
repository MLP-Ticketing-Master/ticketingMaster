package com.ticketmaster.backend.admin.booking.service;

import com.ticketmaster.backend.admin.booking.dto.AdminBookingDetailResponse;
import com.ticketmaster.backend.admin.booking.dto.AdminBookingListResponse;
import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBookingService {
    private final BookingRepository bookingRepository;

    /**
     * 예매 전체 목록 조회 (status 필터)
     * {@code status}가 null이면 전체 예매 조회
     * 최신 등록순(id DESC)으로 정렬됨
     */
    public List<AdminBookingListResponse> findAll(BookingStatus status) {
        return bookingRepository.findAllForAdmin(status).stream()
                .map(AdminBookingListResponse::from)
                .toList();
    }

    /**
     * 예매 상세 조회
     * 존재하지 않으면 {@code BOOKING_NOT_FOUND} 예외 발생
     */
    public AdminBookingDetailResponse findById(Long bookingId) {
        Booking booking = bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        return AdminBookingDetailResponse.from(booking);
    }
}
