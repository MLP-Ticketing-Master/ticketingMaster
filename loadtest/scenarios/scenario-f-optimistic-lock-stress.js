import http from 'k6/http'
import { check, sleep } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 F — Stress (낙관적 락 성능 한계점 탐지)
// 정합성은 절대 깨지지 않음 (Seat.@Version 보장), 성능 측면 한계 측정용
// 사전 조건: queue.enabled=false (큐 우회)
//
// MODE 환경변수로 두 실행 패턴 선택
//   - ramping (기본): RPS 점진 증가 → 시스템 한계점 찾기
//   - constant: 정확히 동시 4000명 → NUM_POPULAR_SEATS=1 과 같이 쓰면 "한 좌석에 동시 4000명"
//
// NUM_POPULAR_SEATS 환경변수로 인기 좌석 수 조절
//   - 10 (기본): 일반 한계점 측정
//   - 1: 한 좌석 워스트케이스

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const MATCH_ID = __ENV.MATCH_ID || 1
const FIRST_SEAT_ID = Number(__ENV.FIRST_SEAT_ID || 1)
const SEAT_ID_STEP = Number(__ENV.SEAT_ID_STEP || 50)
const NUM_POPULAR_SEATS = Number(__ENV.NUM_POPULAR_SEATS || 10)
const POPULAR_SEAT_IDS = Array.from({ length: NUM_POPULAR_SEATS }, (_, i) => FIRST_SEAT_ID + i * SEAT_ID_STEP)

const MODE = (__ENV.MODE || 'ramping').toLowerCase()

export const options = MODE === 'constant' ? {
    scenarios: {
        single_seat_storm: {
            executor: 'constant-vus',
            vus: 4000,                       // 정확히 동시 4000명
            duration: '2m',
        },
    },
} : {
    scenarios: {
        lock_stress: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 1000,
            maxVUs: 5000,
            stages: [
                { duration: '2m', target: 500 },    // 100 → 500 RPS
                { duration: '3m', target: 2000 },   // 500 → 2000 RPS
                { duration: '3m', target: 5000 },   // 2000 → 5000 RPS
                { duration: '2m', target: 5000 },   // 5000 RPS 유지
            ],
        },
    },
    // 한계점 측정용이라 threshold 없음
}

export default function () {
    const row = tokens[__VU % tokens.length]
    const token = row[1]
    const seatId = POPULAR_SEAT_IDS[Math.floor(Math.random() * POPULAR_SEAT_IDS.length)]

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    }

    const res = http.post(
        `http://localhost:8080/matches/${MATCH_ID}/seats/reserve`,
        JSON.stringify({ seatIds: [seatId] }),
        { headers, tags: { name: 'reserve' } }
    )

    check(res, {
        'reserve OK or 409 or 5xx': r => r.status === 200 || r.status === 409 || r.status >= 500,
    })

    // 점유 성공 시 즉시 release — 좌석을 빙빙 돌려서 측정 의미 유지 (DELETE 는 큐 검증 스킵됨)
    if (res.status === 200) {
        http.del(
            `http://localhost:8080/matches/${MATCH_ID}/seats/reserve`,
            JSON.stringify({ seatIds: [seatId] }),
            { headers }
        )
    }
}
