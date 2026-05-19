import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Lock, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLoginMutation } from "@/hooks";
import { toast } from "sonner";
import logo from "@/image/logoNuki.png";
import { ForgotPasswordFlow } from "@/components/main/ForgotPasswordFlow";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keep, setKeep] = useState(false);
  const [showForgot, setShowForgot] = useState(false);
  const login = useLoginMutation();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login.mutate(
      { email, password },
      {
        onSuccess: () => {
          toast.success("로그인되었습니다.");
          navigate("/");
        },
        onError: () => toast.error("로그인에 실패했습니다."),
      },
    );
  };

  return (
    <div className="w-full max-w-md">
      <div className="text-center">
        <div className="flex justify-center">
          <img src={logo} alt="티켓팅마스터" className="h-40 w-auto scale-80" />
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          로그인하여 E스포츠 경기 티켓을 예매하세요
        </p>
      </div>

      <Card className="mt-8 p-8">
        {showForgot ? (
          <ForgotPasswordFlow onBack={() => setShowForgot(false)} />
        ) : (
          <form className="space-y-5" onSubmit={handleSubmit}>
            <FieldWithIcon
              id="email"
              label="이메일"
              icon={Mail}
              type="email"
              placeholder="example@email.com"
              value={email}
              onChange={setEmail}
            />
            <FieldWithIcon
              id="password"
              label="비밀번호"
              icon={Lock}
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={setPassword}
            />

            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2">
                <Checkbox
                  checked={keep}
                  onCheckedChange={(v) => setKeep(!!v)}
                />
                로그인 상태 유지
              </label>
              <button
                type="button"
                onClick={() => setShowForgot(true)}
                className="font-medium text-[#054EFD] hover:underline"
              >
                비밀번호 찾기
              </button>
            </div>

            <Button
              type="submit"
              size="lg"
              disabled={login.isPending}
              className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
            >
              로그인
            </Button>

            <div className="border-t pt-4 text-center text-sm text-muted-foreground">
              아직 회원이 아니신가요?{" "}
              <Link to="/signup" className="font-semibold text-[#054EFD]">
                회원가입
              </Link>
            </div>
          </form>
        )}
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
  onChange: (value: string) => void;
}

function FieldWithIcon({ id, label, icon: Icon, type = "text", placeholder, value, onChange }: FieldProps) {
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
