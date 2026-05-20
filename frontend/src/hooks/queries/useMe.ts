import { useQuery } from "@tanstack/react-query";
import { meApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_USER } from "@/lib/mock";

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export const useMyProfile = () =>
  useQuery({
    queryKey: queryKeys.me.profile,
    queryFn: useMock ? async () => MOCK_USER : () => meApi.profile(),
  });
