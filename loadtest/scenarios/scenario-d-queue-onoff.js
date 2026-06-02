// 시나리오 D — 대기열 ON/OFF 비교
//
// 별도 스크립트가 아니라 "절차" 시나리오, 시나리오 C 를 큐 ON/OFF 두 번 돌려 비교
// (B 는 큐 자체가 측정 대상이라 OFF 런에서 enter 가 무의미해서 제외)
//
// 실행 절차
//   1) application-loadtest.yaml 의 queue.enabled=false 로 변경 후 백엔드 재시작
//   2) 시나리오 C 실행 → 큐 우회로 결제 흐름 직접 측정 (OFF 상태)
//      $env:MATCH_ID = "21101"; $env:FIRST_SEAT_ID = "727051"
//      k6 run --out influxdb=http://localhost:8086/k6 loadtest/scenarios/scenario-c-payment-flow.js
//   3) Grafana 그래프 캡처 (OFF 상태)
//   4) 좌석/booking/Redis 상태 초기화 (좌석 AVAILABLE, booking EXPIRED, Redis FLUSHDB)
//   5) queue.enabled=true 로 복구 후 백엔드 재시작
//   6) 시나리오 C 재실행 → 큐 통과 후 결제 흐름 (ON 상태)
//   7) Grafana 그래프 캡처 (ON 상태)
//   8) before/after 비교 — 큐가 트래픽을 흡수하면 ON 의 응답시간 / 에러율이 더 안정적
//
// (선택) 시나리오 B 는 큐 자체 부하 측정용이라 ON 런 직전에 한 번 더 돌리면
//        "큐가 5k RPS 부하 받는 능력" 도 같이 확인 가능
