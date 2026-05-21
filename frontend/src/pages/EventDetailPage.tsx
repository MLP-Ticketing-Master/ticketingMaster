import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { BookingWidget } from "@/components/main/BookingWidget";
import { EventInfo } from "@/components/main/EventInfo";
import { useEventDetail } from "@/hooks";
import { useAuthStore, useBookingFlowStore } from "@/store";

export default function EventDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const eventId = Number(id);
  const { data: event, isLoading } = useEventDetail(eventId);
  const openFlow = useBookingFlowStore((s) => s.openFlow);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());

  if (isLoading || !event) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-20 text-center text-muted-foreground">
        불러오는 중...
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-10">
      <div className="grid gap-8 lg:grid-cols-[1fr_400px]">
        <div className="space-y-6">
          <div className="aspect-[16/9] overflow-hidden rounded-2xl bg-gray-100">
            <img
              src={event.posterUrl}
              alt={event.title}
              className="h-full w-full object-cover"
            />
          </div>
          <EventInfo event={event} />
        </div>

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
          <BookingNotice />
        </div>
      </div>
    </div>
  );
}

function BookingNotice() {
  return (
    <div className="rounded-2xl bg-blue-50 p-5 text-sm">
      <h4 className="font-semibold">예매 안내</h4>
      <ul className="mt-2 list-disc space-y-1 pl-5 text-muted-foreground">
        <li>1인당 최대 4매까지 예매 가능</li>
        <li>경기 시작 7일 전까지 취소 가능</li>
        <li>미취학 아동 입장 불가</li>
        <li>경기 시작 후 입장 제한</li>
      </ul>
    </div>
  );
}
