import { ArrowLeft, Calendar, MapPin, Receipt, Ticket } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { useBookingDetail, useCancelBookingMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import type { BookingStatus } from "@/types";

const STATUS_LABEL: Record<BookingStatus, string> = {
  CONFIRMED: "예매완료",
  CANCELED: "예매취소",
  PENDING: "결제대기",
  EXPIRED: "만료",
};

const STATUS_BG: Record<BookingStatus, string> = {
  CONFIRMED: "bg-blue-100 text-[#054EFD]",
  CANCELED: "bg-red-100 text-red-600",
  PENDING: "bg-yellow-100 text-yellow-700",
  EXPIRED: "bg-gray-100 text-gray-700",
};

export default function BookingDetailPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const navigate = useNavigate();

  const id = bookingId ? Number(bookingId) : null;
  const { data, isLoading, isError } = useBookingDetail(id);
  const cancel = useCancelBookingMutation();

  const handleCancel = () => {
    if (!data) return;
    if (!confirm("예매를 취소하시겠습니까?")) return;
    cancel.mutate(data.bookingId, {
      onSuccess: () => {
        toast.success("예매가 취소되었습니다.");
        navigate("/my/bookings");
      },
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "예매 취소에 실패했습니다.")),
    });
  };

  if (isLoading) {
    return (
      <Card className="p-8">
        <p className="py-12 text-center text-muted-foreground">불러오는 중...</p>
      </Card>
    );
  }

  if (isError || !data) {
    return (
      <Card className="p-8">
        <p className="py-12 text-center text-red-500">
          예매 정보를 불러오는 데 실패했습니다.
        </p>
        <div className="flex justify-center">
          <Button variant="outline" onClick={() => navigate("/my/bookings")}>
            예매 내역으로
          </Button>
        </div>
      </Card>
    );
  }

  const { matchInfo, seats } = data;
  const matchup =
    matchInfo.homeTeamName && matchInfo.awayTeamName
      ? `${matchInfo.homeTeamName} vs ${matchInfo.awayTeamName}`
      : null;

  const cancellable = data.status === "CONFIRMED";

  return (
    <Card className="space-y-3 px-6 pb-6 pt-4">
      {/* 헤더 — 뒤로가기 + 상태/예매번호 한 줄 */}
      <div>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate("/my/bookings")}
          className="-ml-3 gap-2"
        >
          <ArrowLeft className="h-4 w-4" />
          예매 내역
        </Button>
        <div className="mt-2 flex items-center justify-between">
          <Badge className={STATUS_BG[data.status]}>
            {STATUS_LABEL[data.status]}
          </Badge>
          <p className="text-sm text-muted-foreground">
            예매번호 <span className="font-mono font-medium text-gray-700">{data.bookingNumber}</span>
          </p>
        </div>
      </div>

      {/* 경기 정보 */}
      <section className="space-y-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold">
          <Ticket className="h-4 w-4 text-[#054EFD]" />
          경기 정보
        </h3>
        <Card className="space-y-2 p-5 text-sm">
          <p className="text-base font-semibold">{matchInfo.eventTitle}</p>
          {matchInfo.roundLabel && (
            <p className="text-muted-foreground">{matchInfo.roundLabel}</p>
          )}
          {matchup && (
            <p className="font-medium text-gray-800">{matchup}</p>
          )}
          <Separator />
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Calendar className="h-3.5 w-3.5" />
            {formatDate(matchInfo.startAt)} {formatTime(matchInfo.startAt)}
          </div>
        </Card>
      </section>

      {/* 좌석 정보 */}
      <section className="space-y-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold">
          <MapPin className="h-4 w-4 text-[#054EFD]" />
          좌석 정보 ({seats.length}석)
        </h3>
        <Card className="divide-y">
          {seats.map((s) => (
            <div
              key={s.seatId}
              className="flex items-center justify-between px-5 py-3 text-sm"
            >
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="font-mono">
                  {s.gradeCode}
                </Badge>
                <span className="font-medium">{s.seatCode}</span>
              </div>
              <span className="text-muted-foreground">
                {formatPrice(s.seatPrice)}
              </span>
            </div>
          ))}
        </Card>
      </section>

      {/* 결제 정보 */}
      <section className="space-y-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold">
          <Receipt className="h-4 w-4 text-[#054EFD]" />
          결제 정보
        </h3>
        <Card className="space-y-2 p-5 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>예매 일시</span>
            <span>
              {formatDate(data.createdAt)} {formatTime(data.createdAt)}
            </span>
          </div>
          <Separator />
          <div className="flex items-center justify-between font-bold">
            <span>총 결제 금액</span>
            <span className="text-xl text-[#054EFD]">
              {formatPrice(data.totalPrice)}
            </span>
          </div>
        </Card>
      </section>

      {/* 액션 */}
      {cancellable && (
        <div className="border-t pt-6">
          <Button
            variant="outline"
            size="lg"
            onClick={handleCancel}
            disabled={cancel.isPending}
            className="w-full border-[#1C5EFD] text-[#1C5EFD] hover:bg-blue-50"
          >
            {cancel.isPending ? "취소 처리 중..." : "예매 취소"}
          </Button>
        </div>
      )}
    </Card>
  );
}
