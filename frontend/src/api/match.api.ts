// 백엔드에 독립적인 /matches 목록 엔드포인트가 없음.
// 매치 정보는 GET /events/{eventId} 응답의 matches 배열로 제공됨.
// 이 파일은 하위 호환용으로 유지하되 실제 호출은 event.api.ts를 사용.
export const matchApi = {
  /** @deprecated 백엔드에 /matches 목록 API 없음. eventApi.detail() 사용 */
  list: () =>
    Promise.reject(new Error("Use eventApi.detail() to get matches")),
  /** @deprecated */
  detail: () =>
    Promise.reject(new Error("Use eventApi.detail() to get matches")),
};
