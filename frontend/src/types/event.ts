import type { EventStatus, GameType } from "./common";

export interface EventSummary {
  id: number;
  title: string;
  game: GameType;
  venue: string;
  startDate: string;
  endDate: string;
  posterUrl: string;
  status: EventStatus;
}

export interface EventDetail extends EventSummary {
  description: string;
  durationMinutes: number;
  ageLimit: string;
  prices: TicketPrice[];
  rounds: Round[];
}

export interface TicketPrice {
  gradeCode: string;
  gradeName: string;
  price: number;
  color: string;
}

export interface Round {
  id: number;
  eventId: number;
  roundNo: number;
  matchTitle: string;
  matchUp: string;
  startAt: string;
  status: EventStatus;
  totalSeats: number;
  soldSeats: number;
}
