import http from 'k6/http'
import { check } from 'k6'
import { SharedArray } from 'k6/data'
import { Counter, Rate } from 'k6/metrics'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 A — 좌석 동시성 (메인)
// shared-iterations 1000 VU × 1000 iter = VU 당 1번 점유 시도 (버스트 1회)
// 인기 좌석 50개에 1k VU 가 동시 점유 시도 → Seat.@Version 정합성 + 백엔드 재시도 흡수율 측정
// k6 가 받는 응답은 200 (성공) 또는 409 (이미 점유) 둘 중 하나. 재시도 3회는 백엔드 책임
// 사전 조건: application-loadtest.yaml 의 queue.enabled=false 로 실행 (대기열 우회)
// 성공률 천장 = 50/1000 = 5%. threshold rate>0.04 = "50석 중 40석 이상 정상 예약되면 통과"

const conflictCounter = new Counter('seat_conflict')
const successRate = new Rate('seat_reserve_success')

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const MATCH_ID = __ENV.MATCH_ID || 1
const FIRST_SEAT_ID = Number(__ENV.FIRST_SEAT_ID || 1)
// Hibernate allocationSize=50 으로 좌석 ID 가 50 간격 — 환경변수로 조정 가능
const SEAT_ID_STEP = Number(__ENV.SEAT_ID_STEP || 50)
// 인기 좌석 50개 (시드의 첫 50석)
const POPULAR_SEAT_IDS = Array.from({ length: 50 }, (_, i) => FIRST_SEAT_ID + i * SEAT_ID_STEP)

export const options = {
    scenarios: {
        seat_contention: {
            executor: 'shared-iterations',
            vus: 1000,
            iterations: 1000,
            maxDuration: '2m',
        },
    },
    thresholds: {
        'http_req_duration{name:reserve}': ['p(95)<500'],
        'seat_reserve_success': ['rate>0.04'],
    },
}

export default function () {
    const row = tokens[(__VU - 1) % tokens.length]
    const token = row[1]
    const seatId = POPULAR_SEAT_IDS[Math.floor(Math.random() * POPULAR_SEAT_IDS.length)]

    const res = http.post(
        `http://localhost:8080/matches/${MATCH_ID}/seats/reserve`,
        JSON.stringify({ seatIds: [seatId] }),
        {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            tags: { name: 'reserve' },
        }
    )

    check(res, {
        'reserve OK or conflict': r => r.status === 200 || r.status === 409,
    })

    if (res.status === 200) successRate.add(1)
    else if (res.status === 409) {
        successRate.add(0)
        conflictCounter.add(1)
    }
}
