-- ============================================
-- 부하테스트용 시드 데이터
-- LCK 2026 스프링 개막전 + 좌석 400석 (A~D 4구역) + 유저 1만 명
-- 실행 전 BCRYPT_HASH_HERE 를 BCrypt 해시 문자열로 치환 (회원가입 API 로 발급받은 password 컬럼 값)
-- ============================================
-- SET SERVEROUTPUT ON;  -- SQL*Plus 전용 명령, DBeaver/IntelliJ DB 에선 주석 처리

DECLARE
    v_team_t1     NUMBER;
    v_team_geng   NUMBER;
    v_event_id    NUMBER;
    v_match_id    NUMBER;
    v_section_id  NUMBER;
    v_grade_id    NUMBER;
    v_section_arr SYS.ODCINUMBERLIST := SYS.ODCINUMBERLIST();
BEGIN
    -- (1) Team — 기존 T1, Gen.G 있으면 활용, 없으면 INSERT (멱등성)
    BEGIN
        SELECT id INTO v_team_t1 FROM teams WHERE name = 'T1' AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            INSERT INTO teams (id, name, sport_type, created_at, updated_at)
            VALUES (TEAM_SEQ.NEXTVAL, 'T1', 'LOL', SYSDATE, SYSDATE)
            RETURNING id INTO v_team_t1;
    END;

    BEGIN
        SELECT id INTO v_team_geng FROM teams WHERE name = 'Gen.G' AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            INSERT INTO teams (id, name, sport_type, created_at, updated_at)
            VALUES (TEAM_SEQ.NEXTVAL, 'Gen.G', 'LOL', SYSDATE, SYSDATE)
            RETURNING id INTO v_team_geng;
    END;

    -- (2) Event 1개 (LCK 2026 스프링)
    INSERT INTO events (id, title, sport_type, place, start_date, end_date,
                        max_tickets_per_user, cancel_fee, status, created_at, updated_at)
    VALUES (EVENT_SEQ.NEXTVAL, 'LCK 2026 스프링', 'LOL', 'LoL Park',
            SYSDATE + 7, SYSDATE + 90, 2, 1000, 'OPEN', SYSDATE, SYSDATE)
    RETURNING id INTO v_event_id;

    -- (3) Section 4개 (A/B/C/D 구역)
    FOR i IN 1..4 LOOP
            INSERT INTO sections (id, event_id, name, display_order, description, created_at, updated_at)
            VALUES (SECTION_SEQ.NEXTVAL, v_event_id, CHR(64 + i) || '구역', i, CHR(64 + i) || '구역', SYSDATE, SYSDATE)
            RETURNING id INTO v_section_id;
            v_section_arr.EXTEND;
            v_section_arr(i) := v_section_id;
        END LOOP;

    -- (4) SeatGrade 1개 (VIP 단일, 10만원) — 부하테스트 단순화
    INSERT INTO seat_grades (id, event_id, grade_code, price, color_hex, created_at, updated_at)
    VALUES (SEAT_GRADE_SEQ.NEXTVAL, v_event_id, 'VIP', 100000, '#FF0000', SYSDATE, SYSDATE)
    RETURNING id INTO v_grade_id;

    -- (5) Match 1개 (T1 vs Gen.G)
    INSERT INTO matches (id, event_id, round_label, home_team_id, away_team_id,
                         match_date, start_at, end_at, booking_open_at, booking_close_at,
                         cancel_available_until, status, created_at, updated_at)
    VALUES (MATCH_SEQ.NEXTVAL, v_event_id, '개막전', v_team_t1, v_team_geng,
            SYSDATE + 7, SYSDATE + 7, SYSDATE + 7 + INTERVAL '2' HOUR,
            SYSDATE - 1, SYSDATE + 30, SYSDATE + 6,
            'SCHEDULED', SYSDATE, SYSDATE)
    RETURNING id INTO v_match_id;

    -- (6) Seat 400석 — 4구역 × 10행(A~J) × 10열
    -- seat_code 는 구역까지 포함해 매치 내 고유함 (예: VIP-A1-1 = A구역 1행 1열)
    FOR sec_idx IN 1..4 LOOP
            FOR row_idx IN 1..10 LOOP
                    FOR col_idx IN 1..10 LOOP
                            INSERT INTO seats (id, match_id, section_id, seat_grade_id,
                                               row_label, seat_no, seat_code, status, version, created_at, updated_at)
                            VALUES (SEAT_SEQ.NEXTVAL, v_match_id, v_section_arr(sec_idx), v_grade_id,
                                    CHR(64 + row_idx), col_idx,
                                    'VIP-' || CHR(64 + sec_idx) || row_idx || '-' || col_idx,
                                    'AVAILABLE', 0, SYSDATE, SYSDATE);
                        END LOOP;
                END LOOP;
        END LOOP;

    -- (7) User 1만 명 (1000명마다 COMMIT 으로 UNDO 압박 줄임)
    FOR i IN 1..10000 LOOP
            INSERT INTO users (id, email, password, nickname, role, created_at, updated_at)
            VALUES (USER_SEQ.NEXTVAL, 'loadtest' || i || '@test.com',
                    '$2a$10$yBarrncsBnf8eoZq06P.6eIdF38o5EyyNS1CYybGNAW6T4.zFibCO', 'load' || i, 'USER', SYSDATE, SYSDATE);
            IF MOD(i, 1000) = 0 THEN
                COMMIT;
            END IF;
        END LOOP;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('=== 시드 완료 ===');
    DBMS_OUTPUT.PUT_LINE('match_id = ' || v_match_id);
    DBMS_OUTPUT.PUT_LINE('section_a_id = ' || v_section_arr(1));
    DBMS_OUTPUT.PUT_LINE('grade_id = ' || v_grade_id);
END;
/

-- 확인 쿼리
SELECT COUNT(*) AS user_count FROM users WHERE email LIKE 'loadtest%@test.com';
SELECT COUNT(*) AS seat_count FROM seats WHERE status = 'AVAILABLE';
SELECT MAX(id) AS match_id FROM matches;
SELECT s.id, s.name FROM sections s WHERE s.id IN (SELECT section_id FROM seats WHERE match_id = (SELECT MAX(id) FROM matches));