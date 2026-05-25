import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createEvent, deleteEvent, updateEvent } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type {
  AdminEventCreateRequest,
  AdminEventUpdateRequest,
} from "@/types";

export const useCreateEventMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AdminEventCreateRequest) => createEvent(body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.events }),
  });
};

export const useUpdateEventMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      eventId,
      body,
    }: {
      eventId: number;
      body: AdminEventUpdateRequest;
    }) => updateEvent(eventId, body),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.admin.events });
      qc.invalidateQueries({
        queryKey: queryKeys.admin.eventDetail(vars.eventId),
      });
    },
  });
};

export const useDeleteEventMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (eventId: number) => deleteEvent(eventId),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.admin.events }),
  });
};
