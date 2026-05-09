import { Card } from "@/components/ui/card";
import { BookingItemCard } from "@/components/my/BookingItemCard";
import { useMyBookings, useCancelBookingMutation } from "@/hooks";
import { toast } from "sonner";

export default function BookingHistoryPage() {
  const { data: bookings = [] } = useMyBookings();
  const cancel = useCancelBookingMutation();

  const handleCancel = (id: number) => {
    if (!confirm("예매를 취소하시겠습니까?")) return;
    cancel.mutate(id, {
      onSuccess: () => toast.success("예매가 취소되었습니다."),
    });
  };

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
            <BookingItemCard
              key={b.id}
              booking={b}
              onCancel={handleCancel}
            />
          ))
        )}
      </div>
    </Card>
  );
}
