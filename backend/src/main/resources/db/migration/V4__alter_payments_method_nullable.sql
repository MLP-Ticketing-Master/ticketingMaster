-- 결제 실패 시 토스가 method 를 응답하지 않거나 우리가 모르는 케이스에서도
-- Payment FAILED 기록을 남길 수 있도록 method 컬럼을 NULL 허용으로 변경
-- (엔티티는 이미 nullable = false 미선언 상태였으나 DB 에는 NOT NULL 잔재)

DECLARE
  v_nullable VARCHAR2(1);
BEGIN
  SELECT nullable INTO v_nullable
  FROM user_tab_columns
  WHERE table_name = 'PAYMENTS'
    AND column_name = 'METHOD';

  IF v_nullable = 'N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE payments MODIFY method NULL';
  END IF;
END;
/
