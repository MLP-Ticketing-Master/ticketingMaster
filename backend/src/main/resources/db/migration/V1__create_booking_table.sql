-- booking 테이블 생성 (오라클 12c 이상 자동 증가 PK 문법 적용)
CREATE TABLE booking
(
    id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    NUMBER       NOT NULL,
    match_id   NUMBER       NOT NULL,
    status     VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

-- 인덱스 1: (user_id, status) 복합 인덱스
CREATE INDEX idx_booking_user_status ON booking(user_id, status);

-- 인덱스 2: (match_id, status) 복합 인덱스
CREATE INDEX idx_booking_match_status ON booking(match_id, status);