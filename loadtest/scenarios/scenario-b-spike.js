import http from 'k6/http'
import { check } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 B-spike — 대기열 "순간 폭주" 흡수 검증
// B(ramping)는 한계점/capacity 측정이고, 이건 T=0 부터 즉시 1만 RPS 를 꽂아
// "오픈 순간 절벽을 큐가 흡수하는가" 라는 원래 가설을 직접 검증
//
// 사전 조건
//   - queue.enabled=true 로 백엔드 기동
//   - 측정 전 워밍업 따로 (JVM/풀/Redis 데움) → cold start 와 흡수 실패를 분리
//   - Redis 큐 키 비우고 시작 (FLUSHDB)
//
// 핵심 관전
//   - enter p95 / 에러율 (큐가 받아내면 낮게 유지)
//   - dropped_iterations (거의 0 이어야 k6 가 제대로 1만을 쏜 것 — 높으면 부하생성기 병목)
//   - Redis ZCARD "queue:match:21101" 로 전원 enqueue 됐는지 확인
//
// env 로 조정: RATE(기본 10000), DURATION(기본 30s), PREALLOC, MAXVUS

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const BASE     = __ENV.BASE_URL || 'http://localhost:8080'   // 원격 부하생성 시 서버 LAN IP 를 env 로 주입 (코드엔 안 박음)
const MATCH_ID = __ENV.MATCH_ID || 1
const RATE     = Number(__ENV.RATE || 10000)        // T=0 부터 초당 요청 수
const DURATION = __ENV.DURATION || '30s'
const PREALLOC = Number(__ENV.PREALLOC || 3000)     // 미리 확보할 VU — 부족하면 발사 못 해 결과 오염
const MAXVUS   = Number(__ENV.MAXVUS || 12000)

export const options = {
    scenarios: {
        queue_spike_instant: {
            executor: 'constant-arrival-rate',       // 램프 없이 처음부터 RATE 고정 = 절벽
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PREALLOC,
            maxVUs: MAXVUS,
            gracefulStop: '5s',
        },
    },
    thresholds: {
        'http_req_duration{name:enter}': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
    },
}

export default function () {
    const row = tokens[(__VU - 1) % tokens.length]
    const token = row[1]

    const enterRes = http.post(
        `${BASE}/queue/${MATCH_ID}/enter`,
        null,
        {
            headers: { 'Authorization': `Bearer ${token}` },
            tags: { name: 'enter' },
        }
    )
    check(enterRes, { 'queue enter OK': r => r.status === 200 })
}
