import http from 'k6/http'
import { check, sleep } from 'k6'
import { SharedArray } from 'k6/data'
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js'

// 시나리오 E — 좌석 조회 새로고침 폭주
// 4000 VU 가 좌석 페이지 read API 폭주 → DB pool / Tomcat thread / 캐싱 필요성 측정
// queue.enabled 무관 (조회 API 는 큐 필터 대상 아님)

const tokens = new SharedArray('tokens', () =>
    papaparse.parse(open('../tokens.csv'), { header: false }).data
)

const MATCH_ID = __ENV.MATCH_ID || 1
const SECTION_ID = __ENV.SECTION_ID || 1

export const options = {
    scenarios: {
        refresh_storm: {
            executor: 'constant-vus',
            vus: 4000,                       // worst case 동시 좌석페이지 인원
            duration: '3m',
        },
    },
    thresholds: {
        'http_req_duration{name:sections}': ['p(95)<300'],
        'http_req_duration{name:seats}':    ['p(95)<300'],
        'http_req_failed':                  ['rate<0.01'],
    },
}

export default function () {
    const row = tokens[(__VU - 1) % tokens.length]
    const token = row[1]
    const h = { Authorization: `Bearer ${token}` }

    // 1단계 조회 — 구역 목록 + 등급별 잔여
    http.get(
        `http://localhost:8080/matches/${MATCH_ID}/sections`,
        { headers: h, tags: { name: 'sections' } }
    )
    sleep(0.5)

    // 2단계 조회 — 구역 내 좌석
    http.get(
        `http://localhost:8080/matches/${MATCH_ID}/sections/${SECTION_ID}/seats`,
        { headers: h, tags: { name: 'seats' } }
    )

    // 사용자가 보통 1~3초마다 새로고침한다고 가정
    sleep(Math.random() * 2 + 1)
}
