import { Card } from "@/components/ui/card";
import { BookingItemCard } from "@/components/my/BookingItemCard";
import { useMyBookings, useCancelBookingMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { toast } from "sonner";

export default function BookingHistoryPage() {
  const { data: bookings = [], isLoading, isError } = useMyBookings();
  const cancel = useCancelBookingMutation();

  const handleCancel = (id: number) => {
    if (!confirm("예매를 취소하시겠습니까?")) return;
    cancel.mutate(id, {
      onSuccess: () => toast.success("예매가 취소되었습니다."),
      onError: (err) => {
        toast.error(resolveErrorMessage(err, "예매 취소에 실패했습니다."));
      },
    });
  };

  if (isLoading) {
    return (
      <Card className="p-8">
        <p className="py-12 text-center text-muted-foreground">불러오는 중...</p>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card className="p-8">
        <p className="py-12 text-center text-red-500">
          예매 내역을 불러오는 데 실패했습니다.
        </p>
      </Card>
    );
  }

  return (
    <Card className="space-y-5 p-8">
      <h2 className="text-2xl font-bold">예매 내역</h2>
      <div className="space-y-4">
        {bookings.length === 0 ? (
          <p className="py-12 text-center text-muted-foreground">
            예매 내역이 없습니다.
          </p>
        ) : (
          bookings.map((b) => (
            <BookingItemCard key={b.id} booking={b} onCancel={handleCancel} />
          ))
        )}
      </div>
    </Card>
  );
}
