-- 좌석 유니크 제약 범위 변경
-- (match_id, seat_code) → (match_id, section_id, seat_code)
-- 매치 안에 여러 구역 × 동일 (행, 번호) 좌석이 존재 가능한 도메인 반영

-- 1) 옛 제약 제거 (존재 시) — Hibernate 가 만들었던 uk_seat_match_code
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM user_constraints
  WHERE constraint_name = 'UK_SEAT_MATCH_CODE'
    AND table_name = 'SEATS';
  IF v_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE seats DROP CONSTRAINT uk_seat_match_code';
  END IF;
END;
/

-- 2) 새 제약 추가 (미존재 시) — Hibernate ddl-auto=update 가 먼저 추가했을 가능성 대비
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM user_constraints
  WHERE constraint_name = 'UK_SEAT_MATCH_SECTION_CODE'
    AND table_name = 'SEATS';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE seats ADD CONSTRAINT uk_seat_match_section_code UNIQUE (match_id, section_id, seat_code)';
  END IF;
END;
/
