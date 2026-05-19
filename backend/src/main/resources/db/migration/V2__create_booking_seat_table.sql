-- booking_seat 테이블 생성
CREATE TABLE booking_seat
(
    id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id NUMBER NOT NULL,
    seat_id    NUMBER NOT NULL,
    CONSTRAINT fk_booking_seat_booking_id FOREIGN KEY (booking_id)
        REFERENCES booking (id) ON DELETE CASCADE
);

-- 인덱스 3: (seat_id) 단일 인덱스
CREATE INDEX idx_booking_seat_seat_id ON booking_seat (seat_id);