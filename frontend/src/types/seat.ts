import type { SeatGradeCode, SeatStatus } from "./common";

export interface SeatGrade {
  code: SeatGradeCode;
  name: string;
  price: number;
  color: string;
  sortOrder: number;
  remaining: number;
}

export interface Section {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
}

export interface Seat {
  id: number;
  row: string;
  number: number;
  sectionId: number;
  gradeCode: SeatGradeCode;
  status: SeatStatus;
}

export interface SeatLayout {
  rows: string[];
  cols: number;
  seats: Seat[];
}
