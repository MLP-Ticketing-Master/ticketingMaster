import { useEffect, useRef } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { BookingWidget } from "@/components/event/BookingWidget";
import { EventInfo } from "@/components/event/EventInfo";
import { useEventDetail, useResumeOrStartBooking } from "@/hooks";
import { useAuthStore } from "@/store";
import { SPORT_LABEL } from "@/lib/constants";
import { resolveEventImage } from "@/lib/eventImages";
import { Badge } from "@/components/ui/badge";

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const eventId = Number(id);
  const { data: event, isLoading, isError } = useEventDetail(eventId);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  const resumeOrStart = useResumeOrStartBooking();

  // 결제 실패 페이지 [다시 시도] 진입 — ?resumeBooking=1&matchId=X 로 들어오면 자동 복귀
  const [searchParams, setSearchParams] = useSearchParams();
  const resumeHandledRef = useRef(false);
  useEffect(() => {
    if (resumeHandledRef.current) return;
    if (searchParams.get("resumeBooking") !== "1") return;
    const mid = Number(searchParams.get("matchId"));
    if (!isAuthenticated || !mid) return;
    resumeHandledRef.current = true;
    void resumeOrStart(eventId, mid);
    // 쿼리 정리 — 뒤로가기/리렌더 시 재실행 방지
    setSearchParams({}, { replace: true });
  }, [searchParams, isAuthenticated, eventId, resumeOrStart, setSearchParams]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-20 text-center text-muted-foreground">
        불러오는 중...
      </div>
    );
  }

  if (isError || !event) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-20 text-center text-red-500">
        이벤트 정보를 불러오지 못했습니다.
      </div>
    );
  }

  const sportLabel =
    event.sportType in SPORT_LABEL
      ? SPORT_LABEL[event.sportType as keyof typeof SPORT_LABEL]
      : event.sportType;

  return (
    <div className="mx-auto max-w-7xl px-6 py-10">
      {/* 상단 헤더 */}
      <div className="mb-6 space-y-1">
        <div className="flex items-center gap-2">
          <Badge variant="secondary" className="bg-blue-100 text-[#FF6B47]">
            {sportLabel}
          </Badge>
        </div>
        <h1 className="text-2xl font-bold">{event.title}</h1>
        <p className="text-sm text-muted-foreground">{event.place}</p>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1fr_400px]">
        {/* 좌측: 포스터 + 상세 정보 */}
        <div className="space-y-6">
          <div className="aspect-[16/9] overflow-hidden rounded-2xl bg-gray-100">
            {(() => {
              const src = resolveEventImage(event.detailImageUrl);
              return src ? (
                <img
                  src={src}
                  alt={event.title}
                  className="h-full w-full object-cover object-top"
                />
              ) : (
                <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 to-indigo-200">
                  <span className="text-6xl font-bold text-indigo-400 opacity-30">
                    {event.sportType}
                  </span>
                </div>
              );
            })()}
          </div>
          <EventInfo event={event} />
        </div>

        {/* 우측: 예매 위젯 */}
        <div className="space-y-4">
          <BookingWidget
            event={event}
            onProceed={(matchId) => {
              if (!isAuthenticated) {
                toast.error("로그인이 필요합니다.");
                navigate("/login");
                return;
              }
              const match = event.matches.find((m) => m.matchId === matchId);
              const bookable = match?.bookable ?? match?.isBookable;
              if (!bookable) {
                toast.error("현재 예매 불가능한 회차입니다.");
                return;
              }
              // 미완료 예매 있으면 결제 단계 복귀, 없으면 신규(대기열부터)
              void resumeOrStart(eventId, matchId);
            }}
          />
        </div>
      </div>
    </div>
  );
}
