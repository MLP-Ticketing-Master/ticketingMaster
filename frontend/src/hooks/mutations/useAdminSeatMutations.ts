import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createAdminSeatGrade,
  createAdminSection,
  deleteAdminSeatGrade,
  deleteAdminSection,
  updateAdminSeatGrade,
  updateAdminSection,
  bulkCreateAdminSeats,
  deleteAdminSeat,
  updateAdminSeat,
} from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type {
  AdminSeatBulkCreateRequest,
  AdminSeatGradeCreateRequest,
  AdminSeatGradeUpdateRequest,
  AdminSeatUpdateRequest,
  AdminSectionCreateRequest,
  AdminSectionUpdateRequest,
} from "@/types";

// ── SeatGrade ───────────────────────────────────────────────────

export const useCreateSeatGradeMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AdminSeatGradeCreateRequest) =>
      createAdminSeatGrade(eventId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seatGrades(eventId) }),
  });
};

export const useUpdateSeatGradeMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      seatGradeId,
      body,
    }: {
      seatGradeId: number;
      body: AdminSeatGradeUpdateRequest;
    }) => updateAdminSeatGrade(seatGradeId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seatGrades(eventId) }),
  });
};

export const useDeleteSeatGradeMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (seatGradeId: number) => deleteAdminSeatGrade(seatGradeId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seatGrades(eventId) }),
  });
};

// ── Section ─────────────────────────────────────────────────────

export const useCreateSectionMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AdminSectionCreateRequest) =>
      createAdminSection(eventId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.sections(eventId) }),
  });
};

export const useUpdateSectionMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      sectionId,
      body,
    }: {
      sectionId: number;
      body: AdminSectionUpdateRequest;
    }) => updateAdminSection(sectionId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.sections(eventId) }),
  });
};

export const useDeleteSectionMutation = (eventId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (sectionId: number) => deleteAdminSection(sectionId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.sections(eventId) }),
  });
};

// ── Seat (bulk) ─────────────────────────────────────────────────

export const useBulkCreateSeatsMutation = (matchId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AdminSeatBulkCreateRequest) =>
      bulkCreateAdminSeats(matchId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seats(matchId) }),
  });
};

export const useDeleteSeatMutation = (matchId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (seatId: number) => deleteAdminSeat(seatId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seats(matchId) }),
  });
};

export const useUpdateSeatMutation = (matchId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      seatId,
      body,
    }: {
      seatId: number;
      body: AdminSeatUpdateRequest;
    }) => updateAdminSeat(seatId, body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.seats(matchId) }),
  });
};
