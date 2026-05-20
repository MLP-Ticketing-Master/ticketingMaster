import api from "@/lib/axios";
import type { SeatGrade, SeatLayout, Section } from "@/types";

export const seatApi = {
  grades: (eventId: number) =>
    api
      .get<SeatGrade[]>(`/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  sections: (eventId: number) =>
    api.get<Section[]>(`/events/${eventId}/sections`).then((r) => r.data),
  layout: (roundId: number, sectionId?: number) =>
    api
      .get<SeatLayout>(`/rounds/${roundId}/seats`, {
        params: sectionId ? { sectionId } : {},
      })
      .then((r) => r.data),
};
