import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { Lock, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLoginMutation } from "@/hooks/mutations/auth/useLoginMutation";
import { toast } from "sonner";
import logo from "@/image/logoNuki.png";
import { ForgotPasswordFlow } from "@/components/main/ForgotPasswordFlow";

export default function LoginPage() {
  const navigate = useNavigate();

  // 폼 상태
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showForgot, setShowForgot] = useState(false);

  // 뮤테이션
  const login = useLoginMutation();

  // ===== 검증 함수 =====
  const isEmailValid = (value: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  };

  const isFormValid = !!(
    email.trim() &&
    password.trim() &&
    isEmailValid(email)
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!isFormValid) {
      toast.error("이메일과 비밀번호를 올바르게 입력해주세요");
      return;
    }

    login.mutate(
      { email, password },
      {
        onSuccess: () => {
          toast.success("로그인되었습니다.");
          setEmail("");
          setPassword("");
          navigate("/");
        },
        onError: (error: AxiosError<{ message?: string }>) => {
          const errorMsg =
            error.response?.data?.message ?? "로그인에 실패했습니다.";
          toast.error(errorMsg);
          // 비밀번호 필드 초기화 (보안)
          setPassword("");
        },
      },
    );
  };

  return (
    <div className="w-full max-w-md">
      <div className="text-center">
        <div className="flex justify-center">
          <Link to="/">
            <img
              src={logo}
              alt="티켓팅마스터"
              className="h-40 w-auto scale-80 cursor-pointer"
            />
          </Link>
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

            <div className="flex justify-end text-sm">
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
              disabled={!isFormValid || login.isPending}
              className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
            >
              {login.isPending ? "로그인 중..." : "로그인"}
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

// FieldWithIcon 컴포넌트
interface FieldProps {
  id: string;
  label: string;
  icon: typeof Mail;
  type?: string;
  placeholder?: string;
  value: string;
  onChange: (value: string) => void;
}

function FieldWithIcon({
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
          className="h-11 pl-10 focus-visible:ring-[#054EFD]"
        />
      </div>
    </div>
  );
}
