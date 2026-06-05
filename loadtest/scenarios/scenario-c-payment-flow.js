import http from 'k6/http'
import { check } from 'k6'
import exec from 'k6/execution'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 C — 결제 종단 흐름
// 400 좌석 각각에 대해 정상 점유 → 예매 → 결제 1회씩 완주
// shared-iterations 200 VU × 400 iter — 각 iteration 이 전역 인덱스로 다른 좌석 잡음
// 사전 조건:
//   - queue.enabled=false (큐 우회)
//   - toss.mock-mode=true (TossPaymentsClient mock 분기 동작)
// 반복 실행 시 좌석 상태 reset 필요 (이전 실행에서 SOLD/RESERVED 된 좌석 다시 AVAILABLE)

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const BASE = __ENV.BASE_URL || 'http://localhost:8080'   // 원격 부하생성 시 서버 LAN IP 를 env 로 주입 (코드엔 안 박음)
const MATCH_ID = __ENV.MATCH_ID || 1
const FIRST_SEAT_ID = Number(__ENV.FIRST_SEAT_ID || 1)
const SEAT_ID_STEP = Number(__ENV.SEAT_ID_STEP || 50)
const NUM_SEATS = Number(__ENV.NUM_SEATS || 400)
const SEAT_PRICE = 100000   // 시드 VIP 단일 등급

const SEAT_IDS = Array.from({ length: NUM_SEATS }, (_, i) => FIRST_SEAT_ID + i * SEAT_ID_STEP)

export const options = {
    scenarios: {
        payment_flow: {
            executor: 'shared-iterations',
            vus: 200,
            iterations: NUM_SEATS,             // 400 좌석 한 번씩
            maxDuration: '5m',
        },
    },
    thresholds: {
        'http_req_duration{name:reserve}': ['p(95)<500'],
        'http_req_duration{name:booking}': ['p(95)<500'],
        'http_req_duration{name:payment}': ['p(95)<1000'],
        'checks': ['rate>0.95'],               // 95% 이상의 흐름이 완주
    },
}

export default function () {
    // 전역 iteration 인덱스로 좌석 + 토큰 1:1 매핑 (maxTicketsPerUser 위배 방지)
    const idx = exec.scenario.iterationInTest
    const seatId = SEAT_IDS[idx % NUM_SEATS]
    const row = tokens[idx % tokens.length]
    const token = row[1]

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    }

    // 1) 좌석 점유
    const reserveRes = http.post(
        `${BASE}/matches/${MATCH_ID}/seats/reserve`,
        JSON.stringify({ seatIds: [seatId] }),
        { headers, tags: { name: 'reserve' } }
    )
    if (!check(reserveRes, { 'reserve OK': r => r.status === 200 })) return

    // 2) 예매 생성
    const bookingRes = http.post(
        `${BASE}/bookings`,
        JSON.stringify({ matchId: Number(MATCH_ID), seatIds: [seatId] }),
        { headers, tags: { name: 'booking' } }
    )
    const bookingOk = check(bookingRes, {
        'booking OK': r => r.status === 201 || r.status === 200
    })
    if (!bookingOk) return
    const bookingId = bookingRes.json('bookingId')

    // 3) 결제 confirm (mock)
    const paymentRes = http.post(
        `${BASE}/payments/confirm`,
        JSON.stringify({
            bookingId,
            paymentKey: `loadtest-${idx}`,
            orderId: `order-${idx}`,
            amount: SEAT_PRICE,
        }),
        { headers, tags: { name: 'payment' } }
    )
    check(paymentRes, { 'payment OK': r => r.status === 200 })
}
