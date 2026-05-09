import { http } from "@/lib/axios";
import type { SeatGrade, SeatLayout, Section } from "@/types";

export const seatApi = {
  grades: (eventId: number) =>
    http
      .get<SeatGrade[]>(`/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  sections: (eventId: number) =>
    http.get<Section[]>(`/events/${eventId}/sections`).then((r) => r.data),
  layout: (roundId: number, sectionId?: number) =>
    http
      .get<SeatLayout>(`/rounds/${roundId}/seats`, {
        params: sectionId ? { sectionId } : {},
      })
      .then((r) => r.data),
};
