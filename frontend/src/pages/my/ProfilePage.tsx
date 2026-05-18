import { Card } from "@/components/ui/card";
import { StatCard } from "@/components/my/StatCard";
import { Separator } from "@/components/ui/separator";
import { useMyProfile, useMyStats } from "@/hooks";
import { formatDate } from "@/lib/format";

export default function ProfilePage() {
  const { data: profile } = useMyProfile();
  const { data: stats } = useMyStats();

  if (!profile) return null;

  return (
    <Card className="space-y-6 p-8">
      <h2 className="text-2xl font-bold">내 정보 조회</h2>

      <div className="flex items-center gap-5">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-100 text-2xl font-bold text-[#3C76FE]">
          {profile.nickname.charAt(0)}
        </div>
        <div>
          <p className="text-lg font-bold">{profile.nickname}</p>
          <p className="text-sm text-muted-foreground">
            가입일: {formatDate(profile.joinedAt)}
          </p>
        </div>
      </div>

      <Separator />

      <div className="grid gap-5 md:grid-cols-2">
        <ReadOnlyField label="이메일" value={profile.email} />
        <ReadOnlyField label="휴대폰 번호" value={profile.phone} />
      </div>

      <Separator />

      <div>
        <h3 className="font-bold">이용 통계</h3>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <StatCard value={stats?.totalBookings ?? 0} label="총 예매 횟수" />
          <StatCard value={stats?.upcomingMatches ?? 0} label="예정된 경기" />
          <StatCard value={stats?.watchedMatches ?? 0} label="관람 완료" />
        </div>
      </div>
    </Card>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <div className="space-y-1.5">
      <p className="text-sm text-muted-foreground">{label}</p>
      <div className="rounded-lg bg-gray-50 px-4 py-2.5 text-sm font-medium">
        {value}
      </div>
    </div>
  );
}
