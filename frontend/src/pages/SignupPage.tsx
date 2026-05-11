import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Lock, Mail, Phone, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useSignupMutation } from "@/hooks";
import { toast } from "sonner";

export default function SignupPage() {
  const navigate = useNavigate();
  const signup = useSignupMutation();
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    passwordConfirm: "",
  });
  const [agreed, setAgreed] = useState(false);

  const update = (key: keyof typeof form) => (v: string) =>
    setForm((prev) => ({ ...prev, [key]: v }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreed) return toast.error("약관에 동의해주세요.");
    if (form.password !== form.passwordConfirm)
      return toast.error("비밀번호가 일치하지 않습니다.");
    signup.mutate(form, {
      onSuccess: () => {
        toast.success("회원가입이 완료되었습니다.");
        navigate("/");
      },
    });
  };

  return (
    <div className="-mt-16 w-full max-w-md">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-[#FF6B47]">티켓팅마스터</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          간편하게 가입하고 E스포츠 경기 티켓을 예매하세요
        </p>
      </div>

      <Card className="mt-8 p-8">
        <h2 className="text-xl font-bold">회원가입</h2>
        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <Field
            id="name"
            label="이름"
            icon={User}
            placeholder="홍길동"
            value={form.name}
            onChange={update("name")}
          />
          <Field
            id="email"
            label="이메일"
            icon={Mail}
            type="email"
            placeholder="example@email.com"
            value={form.email}
            onChange={update("email")}
          />
          <Field
            id="phone"
            label="휴대폰 번호"
            icon={Phone}
            placeholder="010-0000-0000"
            value={form.phone}
            onChange={update("phone")}
          />
          <Field
            id="password"
            label="비밀번호"
            icon={Lock}
            type="password"
            placeholder="영문, 숫자 포함 8자 이상"
            value={form.password}
            onChange={update("password")}
          />
          <Field
            id="passwordConfirm"
            label="비밀번호 확인"
            icon={Lock}
            type="password"
            placeholder="비밀번호를 다시 입력하세요"
            value={form.passwordConfirm}
            onChange={update("passwordConfirm")}
          />

          <label className="flex items-center gap-2 pt-2 text-sm">
            <Checkbox
              checked={agreed}
              onCheckedChange={(v) => setAgreed(!!v)}
            />
            <span>
              <Link to="#" className="font-semibold text-[#FF6B47]">
                이용약관
              </Link>{" "}
              및{" "}
              <Link to="#" className="font-semibold text-[#FF6B47]">
                개인정보처리방침
              </Link>
              에 동의합니다
            </span>
          </label>

          <Button
            type="submit"
            size="lg"
            disabled={signup.isPending}
            className="w-full bg-[#FF6B47] hover:bg-[#E5532E]"
          >
            가입하기
          </Button>
        </form>

        <div className="mt-5 border-t pt-4 text-center text-sm text-muted-foreground">
          이미 회원이신가요?{" "}
          <Link to="/login" className="font-semibold text-[#FF6B47]">
            로그인
          </Link>
        </div>
      </Card>
    </div>
  );
}

interface FieldProps {
  id: string;
  label: string;
  icon: typeof Mail;
  type?: string;
  placeholder?: string;
  value: string;
  onChange: (v: string) => void;
}

function Field({
  id,
  label,
  icon: Icon,
  type = "text",
  placeholder,
  value,
  onChange,
}: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Icon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          id={id}
          type={type}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="pl-10"
        />
      </div>
    </div>
  );
}
