export const queryKeys = {
  events: {
    all: ["events"] as const,
    list: (game?: string) => ["events", "list", game ?? "ALL"] as const,
    detail: (id: number) => ["events", "detail", id] as const,
  },
  rounds: {
    all: ["rounds"] as const,
    list: (eventId?: number) => ["rounds", "list", eventId ?? "ALL"] as const,
    seatLayout: (roundId: number) => ["rounds", "seats", roundId] as const,
  },
  bookings: {
    me: ["bookings", "me"] as const,
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
    seatGrades: (eventId: number) =>
      ["admin", "seatGrades", eventId] as const,
    sections: (eventId: number) => ["admin", "sections", eventId] as const,
  },
};
