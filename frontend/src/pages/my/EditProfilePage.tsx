import { useEffect, useState } from "react";
import { Mail, Phone, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useMyProfile, useUpdateProfileMutation } from "@/hooks";
import { toast } from "sonner";

export default function EditProfilePage() {
  const { data: profile } = useMyProfile();
  const update = useUpdateProfileMutation();
  const [form, setForm] = useState({ name: "", email: "", phone: "" });

  useEffect(() => {
    if (profile)
      setForm({
        name: profile.name,
        email: profile.email,
        phone: profile.phone,
      });
  }, [profile]);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    update.mutate(form, {
      onSuccess: () => toast.success("정보가 수정되었습니다."),
    });
  };

  const onWithdraw = () => {
    if (!confirm("정말 탈퇴하시겠습니까?")) return;
    toast.success("탈퇴 처리되었습니다.");
  };

  return (
    <Card className="space-y-6 p-8">
      <h2 className="text-2xl font-bold">회원정보 수정</h2>
      <form className="space-y-5" onSubmit={onSubmit}>
        <Field
          id="name"
          label="이름"
          icon={User}
          value={form.name}
          onChange={(v) => setForm({ ...form, name: v })}
        />
        <Field
          id="email"
          label="이메일"
          icon={Mail}
          type="email"
          value={form.email}
          onChange={(v) => setForm({ ...form, email: v })}
        />
        <Field
          id="phone"
          label="휴대폰 번호"
          icon={Phone}
          value={form.phone}
          onChange={(v) => setForm({ ...form, phone: v })}
        />

        <div className="flex gap-2 pt-4">
          <Button
            type="submit"
            size="lg"
            disabled={update.isPending}
            className="flex-1 bg-[#FF6B47] hover:bg-[#E5532E]"
          >
            저장하기
          </Button>
          <Button
            type="button"
            size="lg"
            variant="outline"
            onClick={onWithdraw}
            className="border-red-400 text-red-500 hover:bg-red-50"
          >
            회원탈퇴
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
}: {
  id: string;
  label: string;
  icon: typeof Mail;
  type?: string;
  value: string;
  onChange: (v: string) => void;
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
        />
      </div>
    </div>
  );
}
