-- bookings.status CHECK 제약에 EXPIRED 값 추가
-- Hibernate 가 @Enumerated(EnumType.STRING) 매핑 시 자동 생성한 CHECK 제약은
-- 처음 만들어질 당시의 enum 값만 허용. enum 에 EXPIRED 가 나중에 추가됐는데
-- ddl-auto=update 가 CHECK 제약은 갱신하지 않아 EXPIRED UPDATE 시 ORA-02290 발생.

-- 1) status 컬럼에 걸린 기존 CHECK 제약을 모두 drop (자동 생성된 SYS_C... 포함)
BEGIN
  FOR c IN (
    SELECT constraint_name
    FROM user_constraints
    WHERE table_name = 'BOOKINGS'
      AND constraint_type = 'C'
      AND UPPER(search_condition_vc) LIKE '%STATUS%'
  ) LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE bookings DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;
END;
/

-- 2) BookingStatus enum 전체 값을 허용하는 CHECK 제약 명시적 이름으로 재생성
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM user_constraints
  WHERE table_name = 'BOOKINGS'
    AND constraint_name = 'BOOKINGS_STATUS_CHECK';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE bookings ADD CONSTRAINT bookings_status_check
                       CHECK (status IN (''PENDING'', ''CONFIRMED'', ''CANCELED'', ''EXPIRED''))';
  END IF;
END;
/
