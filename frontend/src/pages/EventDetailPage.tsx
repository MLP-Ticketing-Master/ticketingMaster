import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { BookingWidget } from "@/components/main/BookingWidget";
import { EventInfo } from "@/components/main/EventInfo";
import { useEventDetail } from "@/hooks";
import { useAuthStore, useBookingFlowStore } from "@/store";
import { SPORT_LABEL } from "@/lib/constants";
import { resolveEventImage } from "@/lib/eventImages";
import { Badge } from "@/components/ui/badge";

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const eventId = Number(id);
  const { data: event, isLoading, isError } = useEventDetail(eventId);
  const openFlow = useBookingFlowStore((s) => s.openFlow);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());

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
              openFlow({ eventId, matchId });
            }}
          />
        </div>
      </div>
    </div>
  );
}
