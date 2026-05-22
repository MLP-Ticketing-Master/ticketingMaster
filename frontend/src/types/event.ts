import type { EventStatus, SportType } from "./common";

// ── GET /events (목록) ──────────────────────────────────────────
export interface EventListResponse {
  eventId: number;
  title: string;
  sportType: SportType;
  place: string;
  thumbnailUrl: string | null;
  startDate: string; // "YYYY-MM-DD"
  endDate: string;
  status: EventStatus;
}

// ── GET /events/{id} (상세) ──────────────────────────────────────
export interface EventDetailResponse {
  title: string;
  sportType: SportType;
  place: string;
  detailImageUrl: string | null;
  description: string | null;
  startDate: string;
  endDate: string;
  matchDurationText: string | null;
  ageRating: string | null;
  bookingNotice: string | null;
  maxTicketsPerUser: number;
  cancelFee: number;
  status: EventStatus;
  seatGrades: SeatGradeResponse[];
  matches: MatchResponse[];
}

export interface SeatGradeResponse {
  seatGradeId: number;
  gradeCode: string;
  price: number;
  colorHex: string;
}

export interface MatchResponse {
  matchId: number;
  roundLabel: string | null;
  matchDate: string;       // "YYYY-MM-DD"
  startAt: string;         // ISO datetime
  bookingOpenAt: string;
  bookingCloseAt: string;
  homeTeam: TeamResponse | null;
  awayTeam: TeamResponse | null;
  status: string;
  isBookable: boolean;
  bookable: boolean; // Java boolean 필드는 Jackson 직렬화 시 "bookable"로 나옴
}

export interface TeamResponse {
  teamId: number;
  name: string;
  logoUrl: string | null;
  sportType: SportType;
}

// ── GET /matches/{matchId}/sections ──────────────────────────────
export interface SeatSectionListResponse {
  matchId: number;
  sections: SectionItem[];
  gradeAvailability: GradeAvailability[];
}

export interface SectionItem {
  sectionId: number;
  name: string;
  displayOrder: number;
  availableCount: number;
}

export interface GradeAvailability {
  gradeCode: string;
  colorHex: string;
  availableCount: number;
}

// ── 하위 호환 alias ──────────────────────────────────────────────
export type EventSummary = EventListResponse;
export type EventDetail = EventDetailResponse;
export interface TicketPrice {
  gradeCode: string;
  gradeName: string;
  price: number;
  color: string;
}
export interface Match extends MatchResponse {}