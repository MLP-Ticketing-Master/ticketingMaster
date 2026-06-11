import http from 'k6/http'
import { check } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 B — 대기열 스파이크
// 5k RPS 큐 진입 부하 — 큐 enter API 가 받아내는지 측정
// 사전 조건: queue.enabled=true 로 실행 (대기열 동작 확인)
//
// 측정 목적
//   - 큐 enter API 의 RPS 유지력 (5k RPS 안 떨어지는지)
//   - 응답시간 분포 (p95)
//   - Redis Sorted Set 메모리 / 크기 증가 곡선
// 승격 처리 시간 (200명/30초 정확도) 은 백엔드 로그 + Redis ZCARD 명령으로 별도 확인
//   docker exec ticketing-master-redis redis-cli ZCARD "queue:match:1"

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const BASE = __ENV.BASE_URL || 'http://localhost:8080'   // 원격 부하생성 시 서버 LAN IP 를 env 로 주입 (코드엔 안 박음)
const MATCH_ID = __ENV.MATCH_ID || 1

export const options = {
    scenarios: {
        queue_spike: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 2000,
            maxVUs: 10000,
            stages: [
                { duration: '10s', target: 5000 },   // 0 → 5000 RPS 로 10초 만에 램프업
                { duration: '30s', target: 5000 },   // 5000 RPS 30초 유지
                { duration: '10s', target: 0 },      // 종료
            ],
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
    },
}

export default function () {
    const row = tokens[(__VU - 1) % tokens.length]
    const token = row[1]

    const enterRes = http.post(
        `${BASE}/queue/${MATCH_ID}/enter`,
        null,
        { headers: { 'Authorization': `Bearer ${token}` } }
    )
    check(enterRes, { 'queue enter OK': r => r.status === 200 })
}
