import http from 'k6/http'
import { check, sleep } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 D — 대기열 ON/OFF 고부하 비교
// 실제 티켓팅 오픈처럼 대규모 동시 인원(기본 5000 VU)이 인기 좌석에 몰릴 때
// 큐 OFF(직접 reserve) vs ON(큐 통과 후 reserve)에서 reserve 단계가 어떻게 달라지는지 비교
//
// QUEUE_MODE 환경변수로 모드 선택 (백엔드 queue.enabled 와 반드시 일치시켜 실행)
//   - off : 큐 우회, reserve 직접 (백엔드 queue.enabled=false)
//   - on  : enter → ALLOWED 대기(폴링) → Queue-Token 으로 reserve (백엔드 queue.enabled=true)
// reserve 성공 시 즉시 release 로 좌석 순환 — 매진 없이 지속 측정
//
// 실행 예)
//   $env:QUEUE_MODE="off"; k6 run --out influxdb=... scenario-d-queue-onoff.js
//   $env:QUEUE_MODE="on";  k6 run --out influxdb=... scenario-d-queue-onoff.js

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const BASE = __ENV.BASE_URL || 'http://localhost:8080'   // 원격 부하생성 시 서버 LAN IP 를 env 로 주입 (코드엔 안 박음)
const MATCH_ID = __ENV.MATCH_ID || 1
const FIRST_SEAT_ID = Number(__ENV.FIRST_SEAT_ID || 1)
const SEAT_ID_STEP = Number(__ENV.SEAT_ID_STEP || 50)
// 인기 좌석 풀 — 좁을수록 경합 심함 (기본 50)
const NUM_POPULAR_SEATS = Number(__ENV.NUM_POPULAR_SEATS || 50)
const POPULAR_SEAT_IDS = Array.from({ length: NUM_POPULAR_SEATS }, (_, i) => FIRST_SEAT_ID + i * SEAT_ID_STEP)

const QUEUE_MODE = (__ENV.QUEUE_MODE || 'off').toLowerCase()
const VUS = Number(__ENV.VUS || 5000)
const DURATION = __ENV.DURATION || '2m'

// 큐 ON 일 때 ALLOWED 대기 폴링 — 과부하 무한 대기 방지 위해 제한
// (3초 × 12회 = 최대 36초 대기, 스케줄러 승격 주기 30초를 한 번 넘김)
const POLL_INTERVAL_SECONDS = 3
const MAX_POLLS = 12

export const options = {
    scenarios: {
        queue_compare: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    // 비교 측정용이라 통과 기준 없음 — OFF/ON 수치 대비가 목적
}

export default function () {
    const token = tokens[__VU % tokens.length][1]
    const auth = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    }
    const seatId = POPULAR_SEAT_IDS[Math.floor(Math.random() * POPULAR_SEAT_IDS.length)]

    let queueToken = null

    // 큐 ON 모드 — 입장 후 ALLOWED 까지 폴링
    if (QUEUE_MODE === 'on') {
        const enterRes = http.post(
            `${BASE}/queue/${MATCH_ID}/enter`,
            null,
            { headers: auth, tags: { name: 'queue_enter' } }
        )
        // 입장 자체 실패(과부하/연결거부) 시 이번 iteration 종료
        if (enterRes.status !== 200) return
        queueToken = enterRes.json('queueToken')
        let status = enterRes.json('status')

        let polls = 0
        while (status === 'WAITING' && polls < MAX_POLLS) {
            sleep(POLL_INTERVAL_SECONDS)
            polls++
            const st = http.get(
                `${BASE}/queue/${MATCH_ID}/status`,
                { headers: { ...auth, 'Queue-Token': queueToken }, tags: { name: 'queue_status' } }
            )
            if (st.status !== 200) break
            status = st.json('status')
        }
        // 아직 WAITING/만료면 reserve 까지 못 감 — 큐가 걸러낸(대기시킨) 인원
        if (status !== 'ALLOWED') return
    }

    // 좌석 점유 — ON 이면 Queue-Token 동봉
    const headers = queueToken ? { ...auth, 'Queue-Token': queueToken } : auth
    const res = http.post(
        `${BASE}/matches/${MATCH_ID}/seats/reserve`,
        JSON.stringify({ seatIds: [seatId] }),
        { headers, tags: { name: 'reserve' } }
    )
    check(res, { 'reserve 200/409/5xx': r => r.status === 200 || r.status === 409 || r.status >= 500 })

    // 점유 성공 시 즉시 release 로 좌석 순환 (DELETE 는 큐 검증 스킵)
    if (res.status === 200) {
        http.del(
            `${BASE}/matches/${MATCH_ID}/seats/reserve`,
            JSON.stringify({ seatIds: [seatId] }),
            { headers: auth }
        )
    }
}
