import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { useMyProfile } from "@/hooks";
import { formatDate } from "@/lib/format";

export default function ProfilePage() {
  const { data: profile } = useMyProfile();

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
