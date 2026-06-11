import { useState } from "react";
import { Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useChangePasswordMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { toast } from "sonner";

// 백엔드 패턴: 8~20자, 영문 + 숫자 + 특수문자(@$!%*#?&) 필수
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;

export default function ChangePasswordPage() {
  const change = useChangePasswordMutation();
  const [form, setForm] = useState({
    currentPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!PASSWORD_PATTERN.test(form.newPassword)) {
      return toast.error(
        "새 비밀번호는 8~20자이며, 영문·숫자·특수문자(@$!%*#?&)를 포함해야 합니다."
      );
    }

    if (form.newPassword !== form.newPasswordConfirm) {
      return toast.error("새 비밀번호가 일치하지 않습니다.");
    }

    change.mutate(form, {
      onSuccess: () => {
        toast.success("비밀번호가 변경되었습니다.");
        setForm({ currentPassword: "", newPassword: "", newPasswordConfirm: "" });
      },
      onError: (err) => {
        toast.error(resolveErrorMessage(err, "비밀번호 변경에 실패했습니다."));
      },
    });
  };

  return (
    <Card className="space-y-6 p-8">
      <h2 className="text-2xl font-bold">비밀번호 변경</h2>

      <form className="space-y-4" onSubmit={onSubmit}>
        <PasswordField
          id="currentPassword"
          label="현재 비밀번호"
          placeholder="현재 비밀번호를 입력하세요"
          value={form.currentPassword}
          onChange={(v) => setForm({ ...form, currentPassword: v })}
        />
        <PasswordField
          id="newPassword"
          label="새 비밀번호"
          placeholder="영문, 숫자, 특수문자 포함 8~20자"
          value={form.newPassword}
          onChange={(v) => setForm({ ...form, newPassword: v })}
        />
        <PasswordField
          id="newPasswordConfirm"
          label="새 비밀번호 확인"
          placeholder="새 비밀번호를 다시 입력하세요"
          value={form.newPasswordConfirm}
          onChange={(v) => setForm({ ...form, newPasswordConfirm: v })}
        />

        {/* 백엔드 @Pattern 조건 반영 */}
        <div className="rounded-lg bg-blue-50 p-4 text-sm">
          <h4 className="font-semibold">비밀번호 설정 조건</h4>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-muted-foreground">
            <li>8자 이상 20자 이하</li>
            <li>영문, 숫자, 특수문자(@$!%*#?&)를 모두 포함</li>
            <li>이전 비밀번호와 다르게 설정</li>
          </ul>
        </div>

        <Button
          type="submit"
          size="lg"
          disabled={change.isPending}
          className="w-full bg-[#054EFD] hover:bg-[#3C76FE]"
        >
          {change.isPending ? "변경 중..." : "비밀번호 변경"}
        </Button>
      </form>
    </Card>
  );
}

function PasswordField({
  id,
  label,
  placeholder,
  value,
  onChange,
}: {
  id: string;
  label: string;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          id={id}
          type="password"
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="pl-10"
        />
      </div>
    </div>
  );
}
