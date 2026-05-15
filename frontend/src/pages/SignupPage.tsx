import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Lock, Mail, User, Phone, AlertCircle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useSignupMutation } from "@/hooks";
import { useAuthStore } from "@/store";
import { toast } from "sonner";
import logo from "@/image/logoNuki.png";
 
export default function SignupPage() {
  const navigate = useNavigate();
  const { error: authError, setError } = useAuthStore();
 
  // 폼 상태
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    passwordConfirm: "",
  });
 
  // 검증 상태
  const [touched, setTouched] = useState({
    name: false,
    email: false,
    phone: false,
    password: false,
    passwordConfirm: false,
  });
 
  const [validations, setValidations] = useState({
    emailValid: false,
    passwordStrong: false,
    passwordMatch: false,
  });
 
  // 뮤테이션
  const signup = useSignupMutation();
 
  // ===== 검증 함수 =====
  const isEmailValid = (email: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };
 
  const isPasswordStrong = (password: string) => {
    return (
      password.length >= 8 &&
      /[A-Z]/.test(password) &&
      /[a-z]/.test(password) &&
      /[0-9]/.test(password) &&
      /[!@#$%^&*(),.?":{}|<>]/.test(password)
    );
  };
 
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
 
    // 실시간 검증
    if (name === "email") {
      const valid = isEmailValid(value);
      setValidations((prev) => ({ ...prev, emailValid: valid }));
    }
 
    if (name === "password") {
      const strong = isPasswordStrong(value);
      const match = value === formData.passwordConfirm;
      setValidations((prev) => ({
        ...prev,
        passwordStrong: strong,
        passwordMatch: match,
      }));
    }
 
    if (name === "passwordConfirm") {
      const match = value === formData.password;
      setValidations((prev) => ({ ...prev, passwordMatch: match }));
    }
 
    // 에러 메시지 초기화
    if (authError) {
      setError(null);
    }
  };
 
  const handleBlur = (field: keyof typeof touched) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
  };
 
  const isFormValid =
    validations.emailValid &&
    validations.passwordStrong &&
    validations.passwordMatch &&
    formData.name.trim() &&
    formData.phone.trim();
 
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
 
    if (!isFormValid) {
      toast.error("모든 필드를 올바르게 입력해주세요");
      return;
    }
 
    signup.mutate(
      {
        name: formData.name,
        email: formData.email,
        phone: formData.phone,
        password: formData.password,
        passwordConfirm: formData.passwordConfirm,
      },
      {
        onSuccess: () => {
          toast.success("회원가입이 완료되었습니다.");
          navigate("/");
        },
        onError: (error: any) => {
          const errorMsg = error.message || "회원가입에 실패했습니다.";
          toast.error(errorMsg);
          // 비밀번호 필드 초기화
          setFormData((prev) => ({
            ...prev,
            password: "",
            passwordConfirm: "",
          }));
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
          새로운 계정을 만들어 E스포츠 경기 티켓을 예매하세요
        </p>
      </div>
 
      <Card className="mt-8 space-y-5 p-8">
        {/* 에러 메시지 */}
        {authError && (
          <div className="flex gap-3 rounded-lg bg-red-50 p-3 text-sm dark:bg-red-900/20">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600 dark:text-red-400" />
            <div>
              <p className="font-medium text-red-800 dark:text-red-200">
                회원가입 실패
              </p>
              <p className="text-red-700 dark:text-red-300">{authError}</p>
            </div>
          </div>
        )}
 
        <form className="space-y-4" onSubmit={handleSubmit}>
          {/* 이름 필드 */}
          <FieldWithIcon
            id="name"
            label="이름"
            icon={User}
            placeholder="홍길동"
            value={formData.name}
            onChange={handleChange}
            onBlur={() => handleBlur("name")}
            disabled={signup.isPending}
            required
          />
 
          {/* 이메일 필드 */}
          <div>
            <FieldWithIcon
              id="email"
              label="이메일"
              icon={Mail}
              type="email"
              placeholder="example@email.com"
              value={formData.email}
              onChange={handleChange}
              onBlur={() => handleBlur("email")}
              disabled={signup.isPending}
              error={
                touched.email && formData.email && !isEmailValid(formData.email)
                  ? "유효한 이메일 형식이 아닙니다"
                  : undefined
              }
            />
            {touched.email && validations.emailValid && (
              <p className="mt-1 flex items-center gap-1 text-xs font-medium text-green-600">
                <CheckCircle2 className="h-3.5 w-3.5" />
                사용 가능한 이메일입니다
              </p>
            )}
          </div>
 
          {/* 전화번호 필드 */}
          <FieldWithIcon
            id="phone"
            label="전화번호"
            icon={Phone}
            type="tel"
            placeholder="010-0000-0000"
            value={formData.phone}
            onChange={handleChange}
            onBlur={() => handleBlur("phone")}
            disabled={signup.isPending}
            required
          />
 
          {/* 비밀번호 필드 */}
          <div>
            <FieldWithIcon
              id="password"
              label="비밀번호"
              icon={Lock}
              type="password"
              placeholder="최소 8자, 대소문자, 숫자, 특수문자 포함"
              value={formData.password}
              onChange={handleChange}
              onBlur={() => handleBlur("password")}
              disabled={signup.isPending}
            />
 
            {/* 비밀번호 요구사항 */}
            {touched.password && formData.password && (
              <div className="mt-2 space-y-1 text-xs">
                <RequirementItem
                  met={formData.password.length >= 8}
                  text="최소 8자 이상"
                />
                <RequirementItem
                  met={/[A-Z]/.test(formData.password)}
                  text="대문자 포함"
                />
                <RequirementItem
                  met={/[a-z]/.test(formData.password)}
                  text="소문자 포함"
                />
                <RequirementItem
                  met={/[0-9]/.test(formData.password)}
                  text="숫자 포함"
                />
                <RequirementItem
                  met={/[!@#$%^&*(),.?":{}|<>]/.test(formData.password)}
                  text="특수문자 포함"
                />
              </div>
            )}
          </div>
 
          {/* 비밀번호 확인 필드 */}
          <FieldWithIcon
            id="passwordConfirm"
            label="비밀번호 확인"
            icon={Lock}
            type="password"
            placeholder="비밀번호를 다시 입력해주세요"
            value={formData.passwordConfirm}
            onChange={handleChange}
            onBlur={() => handleBlur("passwordConfirm")}
            disabled={signup.isPending}
            error={
              touched.passwordConfirm &&
              formData.passwordConfirm &&
              !validations.passwordMatch
                ? "비밀번호가 일치하지 않습니다"
                : undefined
            }
          />
 
          {/* 가입 버튼 */}
          <Button
            type="submit"
            size="lg"
            disabled={!isFormValid || signup.isPending}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {signup.isPending ? "가입 중..." : "회원가입"}
          </Button>
        </form>
 
        {/* 로그인 링크 */}
        <div className="border-t pt-4 text-center text-sm text-muted-foreground">
          이미 회원이신가요?{" "}
          <Link to="/login" className="font-semibold text-[#054EFD] hover:underline">
            로그인
          </Link>
        </div>
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
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: () => void;
  error?: string;
  disabled?: boolean;
  required?: boolean;
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
  required,
}: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>
        {label}
        {required && <span className="text-red-500">*</span>}
      </Label>
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
          onChange={onChange}
          onBlur={onBlur}
          disabled={disabled}
          className={`pl-10 ${
            error
              ? "border-red-500 focus-visible:ring-red-500"
              : "focus-visible:ring-[#054EFD]"
          }`}
        />
      </div>
      {error && <p className="text-xs font-medium text-red-500">{error}</p>}
    </div>
  );
}
 
// RequirementItem 컴포넌트
 
function RequirementItem({ met, text }: { met: boolean; text: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <div
        className={`h-1.5 w-1.5 rounded-full ${
          met ? "bg-green-600" : "bg-gray-300"
        }`}
      />
      <span className={met ? "text-green-600" : "text-muted-foreground"}>
        {text}
      </span>
    </div>
  );
}