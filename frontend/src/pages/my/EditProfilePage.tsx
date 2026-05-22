import { useState } from "react";
import { Mail, Phone, User as UserIcon } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  useMyProfile,
  useUpdateProfileMutation,
  useWithdrawMutation,
} from "@/hooks";
import { toast } from "sonner";
import type { User } from "@/types";

export default function EditProfilePage() {
  const { data: profile } = useMyProfile();

  if (!profile) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        불러오는 중...
      </Card>
    );
  }

  return <EditForm profile={profile} />;
}

// 폼 본체 — profile 이 확정된 시점에만 마운트되므로
// useEffect 로 외부 데이터를 state 에 복사하지 않고 useState 초기값으로 한 번에 결정
function EditForm({ profile }: { profile: User }) {
  const navigate = useNavigate();
  const update = useUpdateProfileMutation();
  const withdraw = useWithdrawMutation();

  // 백엔드 UpdateUserRequest: nickname, phone 만 수정 가능 (email 변경 불가)
  const [form, setForm] = useState({
    nickname: profile.nickname,
    phone: profile.phone,
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    update.mutate(form, {
      onSuccess: () => toast.success("정보가 수정되었습니다."),
      onError: (err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response
            ?.data?.message ?? "수정에 실패했습니다.";
        toast.error(msg);
      },
    });
  };

  const onWithdraw = () => {
    if (!confirm("정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) return;
    withdraw.mutate(undefined, {
      onSuccess: () => {
        toast.success("탈퇴 처리되었습니다.");
        navigate("/");
      },
      onError: (err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response
            ?.data?.message ?? "탈퇴 처리에 실패했습니다.";
        toast.error(msg);
      },
    });
  };

  return (
    <Card className="space-y-6 p-8">
      <h2 className="text-2xl font-bold">회원정보 수정</h2>
      <form className="space-y-5" onSubmit={onSubmit}>
        {/* 이름(닉네임) — 수정 가능 */}
        <Field
          id="nickname"
          label="이름"
          icon={UserIcon}
          value={form.nickname}
          onChange={(v) => setForm({ ...form, nickname: v })}
        />

        {/* 이메일 — 읽기전용 (백엔드 변경 미지원) */}
        <div className="space-y-2">
          <Label htmlFor="email">이메일</Label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="email"
              type="email"
              value={profile.email}
              readOnly
              disabled
              className="cursor-not-allowed bg-gray-50 pl-10 text-muted-foreground"
            />
          </div>
          <p className="text-xs text-muted-foreground">
            이메일은 변경할 수 없습니다.
          </p>
        </div>

        {/* 휴대폰 번호 — 수정 가능 */}
        <Field
          id="phone"
          label="휴대폰 번호"
          icon={Phone}
          value={form.phone}
          onChange={(v) => setForm({ ...form, phone: v })}
          placeholder="010-1234-5678"
        />

        <div className="flex gap-2 pt-4">
          <Button
            type="submit"
            size="lg"
            disabled={update.isPending}
            className="flex-1 bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            {update.isPending ? "저장 중..." : "저장하기"}
          </Button>
          <Button
            type="button"
            size="lg"
            variant="outline"
            onClick={onWithdraw}
            disabled={withdraw.isPending}
            className="border-red-400 text-red-500 hover:bg-red-50"
          >
            {withdraw.isPending ? "처리 중..." : "회원탈퇴"}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function Field({
  id,
  label,
  icon: Icon,
  type = "text",
  value,
  onChange,
  placeholder,
}: {
  id: string;
  label: string;
  icon: typeof Mail;
  type?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Icon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          id={id}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="pl-10"
          placeholder={placeholder}
        />
      </div>
    </div>
  );
}
