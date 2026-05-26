-- booking_seats 의 (match_id, seat_id) UNIQUE 제약 제거
-- 좌석 점유 불변량은 Seat.status + @Version 으로 강제되므로 BookingSeat UK 는 중복 보호 + 부작용
-- 부작용: CONFIRMED → CANCELED 후 같은 좌석 재예매 시도 시 DB UK 위반으로 차단됨

-- 1) UNIQUE 제약 제거 (존재 시) — Hibernate 가 생성했던 uk_booking_seat_match_seat
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM user_constraints
  WHERE constraint_name = 'UK_BOOKING_SEAT_MATCH_SEAT'
    AND table_name = 'BOOKING_SEATS';
  IF v_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE booking_seats DROP CONSTRAINT uk_booking_seat_match_seat';
  END IF;
END;
/

-- 2) match_id 컬럼 제거 (존재 시) — UK 비정규화 전용 컬럼이었음
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM user_tab_columns
  WHERE column_name = 'MATCH_ID'
    AND table_name = 'BOOKING_SEATS';
  IF v_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE booking_seats DROP COLUMN match_id';
  END IF;
END;
/
