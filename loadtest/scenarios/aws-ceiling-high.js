import http from 'k6/http'
import { check } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// AWS 천장 탐색 (high) — 응답캐시 적용 후 더 높은 RPS 까지 상향
// 깨지는 지점 = 새 천장. abortOnFail 로 망가지면 자동 중단
const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)
const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const MATCH_ID = __ENV.MATCH_ID || 1
const SECTION_ID = __ENV.SECTION_ID || 1

export const options = {
    scenarios: {
        ceiling: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 3000,
            stages: [
                { duration: '30s', target: 300 },
                { duration: '30s', target: 600 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 1500 },
                { duration: '30s', target: 2000 },
                { duration: '30s', target: 3000 },
            ],
        },
    },
    thresholds: {
        http_req_failed: [{ threshold: 'rate<0.10', abortOnFail: true, delayAbortEval: '10s' }],
    },
}

export default function () {
    const row = tokens[(__VU - 1) % tokens.length]
    const token = row[1]
    const res = http.get(
        `${BASE}/matches/${MATCH_ID}/sections/${SECTION_ID}/seats`,
        { headers: { Authorization: `Bearer ${token}` }, tags: { name: 'seats' } }
    )
    check(res, { 'status 200': r => r.status === 200 })
}
