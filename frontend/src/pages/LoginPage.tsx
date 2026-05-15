import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Lock, Mail, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useLoginMutation } from "@/hooks";
import { useAuthStore } from "@/store";
import { toast } from "sonner";
import logo from "@/image/logoNuki.png";
 
export default function LoginPage() {
  const navigate = useNavigate();
  const { error: authError, setError } = useAuthStore();
  
  // 폼 상태
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keep, setKeep] = useState(false);
  
  // 검증 상태
  const [touched, setTouched] = useState({
    email: false,
    password: false,
  });
  
  // 뮤테이션
  const login = useLoginMutation();
 
  // ===== 검증 함수 =====
  const isEmailValid = (value: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  };
 
  const isFormValid = email.trim() && password.trim() && isEmailValid(email);
 
  const handleEmailChange = (value: string) => {
    setEmail(value);
    // 에러 메시지 자동 초기화
    if (authError) {
      setError(null);
    }
  };
 
  const handlePasswordChange = (value: string) => {
    setPassword(value);
    // 에러 메시지 자동 초기화
    if (authError) {
      setError(null);
    }
  };
 
  const handleBlur = (field: "email" | "password") => {
    setTouched((prev) => ({ ...prev, [field]: true }));
  };
 
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
 
    // 최종 검증
    if (!isFormValid) {
      toast.error("이메일과 비밀번호를 올바르게 입력해주세요");
      return;
    }
 
    // 로그인 요청
    login.mutate(
      { email, password },
      {
        onSuccess: () => {
          toast.success("로그인되었습니다.");
          // 메모리 초기화
          setEmail("");
          setPassword("");
          // keep이 false면 setKeep도 false로 초기화 (localStorage는 이미 관리됨)
          navigate("/");
        },
        onError: (error: any) => {
          const errorMsg = error.message || "로그인에 실패했습니다.";
          toast.error(errorMsg);
          // 비밀번호 필드 초기화 (보안)
          setPassword("");
          setTouched((prev) => ({ ...prev, password: false }));
        },
      }
    );
  };
 
  return (
    <div className="w-full max-w-md">
      <div className="text-center">
        <div className="flex justify-center">
          <img
            src={logo}
            alt="티켓팅마스터"
            className="h-40 w-auto scale-80"
          />
        </div>
        <p className="mt-2 text-sm text-muted-foreground">
          로그인하여 E스포츠 경기 티켓을 예매하세요
        </p>
      </div>
 
      <Card className="mt-8 space-y-5 p-8">
        {/* 에러 메시지 표시 */}
        {authError && (
          <div className="flex gap-3 rounded-lg bg-red-50 p-3 text-sm dark:bg-red-900/20">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600 dark:text-red-400" />
            <div>
              <p className="font-medium text-red-800 dark:text-red-200">
                로그인 실패
              </p>
              <p className="text-red-700 dark:text-red-300">{authError}</p>
            </div>
          </div>
        )}
 
        <form className="space-y-5" onSubmit={handleSubmit}>
          {/* 이메일 필드 */}
          <FieldWithIcon
            id="email"
            label="이메일"
            icon={Mail}
            type="email"
            placeholder="example@email.com"
            value={email}
            onChange={handleEmailChange}
            onBlur={() => handleBlur("email")}
            error={
              touched.email && email && !isEmailValid(email)
                ? "유효한 이메일 형식이 아닙니다"
                : undefined
            }
            disabled={login.isPending}
          />
 
          {/* 비밀번호 필드 */}
          <FieldWithIcon
            id="password"
            label="비밀번호"
            icon={Lock}
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={handlePasswordChange}
            onBlur={() => handleBlur("password")}
            disabled={login.isPending}
          />
 
          {/* 로그인 상태 유지 & 비밀번호 찾기 */}
          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2">
              <Checkbox
                checked={keep}
                onCheckedChange={(v) => setKeep(!!v)}
                disabled={login.isPending}
              />
              <span>로그인 상태 유지</span>
            </label>
            <Link
              to="/password-reset"
              className="font-medium text-[#054EFD] hover:underline"
            >
              비밀번호 찾기
            </Link>
          </div>
 
          {/* 로그인 버튼 */}
          <Button
            type="submit"
            size="lg"
            disabled={!isFormValid || login.isPending}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {login.isPending ? "로그인 중..." : "로그인"}
          </Button>
        </form>
 
        {/* 회원가입 링크 */}
        <div className="border-t pt-4 text-center text-sm text-muted-foreground">
          아직 회원이 아니신가요?{" "}
          <Link to="/signup" className="font-semibold text-[#054EFD] hover:underline">
            회원가입
          </Link>
        </div>
      </Card>
    </div>
  );
}
 
//FieldWithIcon 컴포넌트
 
interface FieldProps {
  id: string;
  label: string;
  icon: typeof Mail;
  type?: string;
  placeholder?: string;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  error?: string;
  disabled?: boolean;
}
 
function FieldWithIcon({
  id,
  label,
  icon: Icon,
  type = "text",
  placeholder,
  value,
  onChange,
  onBlur,
  error,
  disabled,
}: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Icon
          className={`absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 ${
            error ? "text-red-500" : "text-muted-foreground"
          }`}
        />
        <Input
          id={id}
          type={type}
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onBlur={onBlur}
          disabled={disabled}
          className={`pl-10 ${
            error
              ? "border-red-500 focus-visible:ring-red-500"
              : "focus-visible:ring-[#054EFD]"
          }`}
        />
      </div>
      {/* 에러 메시지 */}
      {error && <p className="text-xs font-medium text-red-500">{error}</p>}
    </div>
  );
}