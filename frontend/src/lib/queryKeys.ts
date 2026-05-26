export const queryKeys = {
  events: {
    all: ["events"] as const,
    list: (game?: string) => ["events", "list", game ?? "ALL"] as const,
    detail: (id: number) => ["events", "detail", id] as const,
  },
  matches: {
    all: ["matches"] as const,
    list: (eventId?: number) => ["matches", "list", eventId ?? "ALL"] as const,
    sections: (matchId: number) =>
      ["matches", "sections", matchId] as const,
    sectionSeats: (matchId: number, sectionId: number) =>
      ["matches", "sections", matchId, "seats", sectionId] as const,
  },
  bookings: {
    me: ["bookings", "me"] as const,
    detail: (id: number) => ["bookings", "detail", id] as const,
    admin: (q?: string, status?: string, page?: number) =>
      ["bookings", "admin", q ?? "", status ?? "ALL", page ?? 0] as const,
  },
  teams: {
    all: ["teams"] as const,
    list: (game?: string) => ["teams", "list", game ?? "ALL"] as const,
  },
  me: {
    profile: ["me", "profile"] as const,
    stats: ["me", "stats"] as const,
  },
  admin: {
    dashboard: ["admin", "dashboard"] as const,
    events: ["admin", "events"] as const,
    eventDetail: (id: number) => ["admin", "events", "detail", id] as const,
    matches: (eventId?: number) =>
      ["admin", "matches", eventId ?? "ALL"] as const,
    matchDetail: (id: number) => ["admin", "matches", "detail", id] as const,
    seatGrades: (eventId: number) =>
      ["admin", "seatGrades", eventId] as const,
    sections: (eventId: number) => ["admin", "sections", eventId] as const,
  },
  queue: {
    // queueToken 을 key 에 포함 — 토큰 만료 후 새 토큰으로 진입 시
    // 이전 토큰의 stale error 가 끼어들지 않도록 캐시 슬롯을 분리
    status: (matchId: number, queueToken: string | null) =>
      ["queue", "status", matchId, queueToken ?? ""] as const,
  },
};
