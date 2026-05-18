package com.ticketmaster.backend.domain.booking.service;

import com.ticketmaster.backend.domain.booking.dto.request.BookingCreateRequest;
import com.ticketmaster.backend.domain.booking.dto.response.BookingResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingSummaryResponse;
import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingSeat;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------
    // 예매 생성
    // -------------------------------------------------------

    @Transactional
    public BookingResponse createBooking(Long userId, BookingCreateRequest request) {
        // 1. 회차 조회
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 좌석 일괄 조회 (SeatGrade fetch join — price 계산용)
        List<Seat> seats = seatRepository.findByMatchAndIdIn(request.getMatchId(), request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }

        // 4. 좌석 점유 검증
        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            // 상태가 RESERVED 가 아니거나 본인 점유가 아니면 → 409
            if (seat.getStatus() != SeatStatus.RESERVED || !userId.equals(seat.getReservedBy())) {
                throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
            }
            // 점유 시간 만료 → 410
            if (seat.getReservedUntil() == null || !now.isBefore(seat.getReservedUntil())) {
                throw new BusinessException(ErrorCode.SEAT_RESERVATION_EXPIRED);
            }
        }

        // 5. totalPrice 계산
        int totalPrice = seats.stream()
                .mapToInt(s -> s.getSeatGrade().getPrice())
                .sum();

        // 6. Booking 생성
        String bookingNumber = generateBookingNumber();
        Booking booking = Booking.create(user, match, bookingNumber, totalPrice);

        // 7. BookingSeat 생성 (가격 스냅샷 저장)
        for (Seat seat : seats) {
            BookingSeat bookingSeat = BookingSeat.of(seat, seat.getSeatGrade().getPrice());
            booking.addBookingSeat(bookingSeat);
        }

        // 8. 저장
        Booking savedBooking = bookingRepository.save(booking);

        // 9. 저장 후 상세 응답 조회 (수정된 부분: 원본 booking이 아닌 savedBooking의 getId() 사용)
        Booking saved = bookingRepository.findDetailByIdForUser(savedBooking.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        return BookingResponse.from(saved);
    }

    // -------------------------------------------------------
    // 단건 조회
    // -------------------------------------------------------

    public BookingResponse getBooking(Long userId, boolean isAdmin, Long bookingId) {
        Booking booking = bookingRepository.findDetailByIdForUser(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_ACCESS_DENIED);
        }

        return BookingResponse.from(booking);
    }

    // -------------------------------------------------------
    // 내 예매 목록
    // -------------------------------------------------------

    public Page<BookingSummaryResponse> getMyBookings(Long userId, BookingStatus status, Pageable pageable) {
        return bookingRepository.findMyBookings(userId, status, pageable)
                .map(BookingSummaryResponse::from);
    }

    // -------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------

    private String generateBookingNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "BK" + date + uid;
    }
}