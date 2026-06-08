import { useState, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AlertCircle, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useSignupMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { useAuthStore } from "@/store";
import { toast } from "sonner";
import logo from "@/assets/logoNuki.png";
import { checkEmailDuplicate } from "@/api/auth";
 
 
export default function SignupPage() {
  const navigate = useNavigate();
  const { error: authError, setError } = useAuthStore();
 
  // ===== 폼 상태 =====
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    passwordConfirm: "",
    nickname: "",  
    phone: "",
  });
 
  // ===== 터치 상태 =====
  const [touched, setTouched] = useState({
    email: false,
    password: false,
    passwordConfirm: false,
    nickname: false,
    phone: false,
  });
 
  // ===== 검증 상태 =====
  const [validations, setValidations] = useState({
    emailValid: false,
    passwordStrong: false,
    passwordMatch: false,
    nicknameValid: false,
    phoneValid: true,
  });
 
  // ===== 이메일 중복 확인 상태 =====
  const [emailCheck, setEmailCheck] = useState<{
    status: "idle" | "checking" | "available" | "duplicate";
    checkedEmail: string;
  }>({ status: "idle", checkedEmail: "" });

  // ===== 뮤테이션 =====
  const signup = useSignupMutation();

  // ===== 이메일 중복 확인 =====
  const handleEmailCheck = useCallback(async (email: string) => {
    if (!isEmailValid(email)) return;
    // 이미 확인한 이메일과 동일하면 재요청 생략
    if (emailCheck.checkedEmail === email && emailCheck.status !== "idle") return;

    setEmailCheck({ status: "checking", checkedEmail: email });
    try {
      const available = await checkEmailDuplicate(email);
      setEmailCheck({
        status: available ? "available" : "duplicate",
        checkedEmail: email,
      });
    } catch {
      // 네트워크 오류 등 — idle로 되돌려 재시도 가능하게
      setEmailCheck({ status: "idle", checkedEmail: "" });
      toast.error("이메일 확인 중 오류가 발생했습니다. 다시 시도해주세요.");
    }
  }, [emailCheck.checkedEmail, emailCheck.status]);
 
  // ===== 검증 함수들 =====
 
  const isEmailValid = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  };
 
  const isPasswordStrong = (password: string): boolean => {
    const regex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;
    return regex.test(password);
  };
 
  const getPasswordStrength = (password: string) => {
    return {
      minLength: password.length >= 8,
      maxLength: password.length <= 20,
      hasLetter: /[A-Za-z]/.test(password),
      hasNumber: /\d/.test(password),
      hasSpecial: /[@$!%*#?&]/.test(password),
    };
  };
 
  const isNicknameValid = (name: string): boolean => {
    return name.length >= 2 && name.length <= 20;
  };
 
  const isPhoneValid = (phone: string): boolean => {
    if (phone === "") return true;
    return /^010-\d{4}-\d{4}$/.test(phone);
  };

  // 숫자만 추출 후 010-XXXX-XXXX 형식으로 자동 하이픈 부여
  const formatPhone = (raw: string): string => {
    const digits = raw.replace(/\D/g, "").slice(0, 11);
    if (digits.length < 4) return digits;
    if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  };
 
  // ===== 입력 처리 =====
  
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    // 전화번호는 입력 즉시 010-XXXX-XXXX 로 포매팅
    const nextValue = name === "phone" ? formatPhone(value) : value;

    setFormData((prev) => ({ ...prev, [name]: nextValue }));

    // 실시간 검증
    switch (name) {
      case "email":
        setValidations((prev) => ({
          ...prev,
          emailValid: isEmailValid(nextValue),
        }));
        // 이메일 값이 바뀌면 중복 확인 결과 초기화
        setEmailCheck({ status: "idle", checkedEmail: "" });
        break;

      case "password": {
        const strong = isPasswordStrong(nextValue);
        const match = nextValue === formData.passwordConfirm;
        setValidations((prev) => ({
          ...prev,
          passwordStrong: strong,
          passwordMatch: match,
        }));
        break;
      }

      case "passwordConfirm":
        setValidations((prev) => ({
          ...prev,
          passwordMatch: nextValue === formData.password,
        }));
        break;

      case "nickname":
        setValidations((prev) => ({
          ...prev,
          nicknameValid: isNicknameValid(nextValue),
        }));
        break;

      case "phone":
        setValidations((prev) => ({
          ...prev,
          phoneValid: isPhoneValid(nextValue),
        }));
        break;
    }
 
    if (authError) {
      setError(null);
    }
  };
 
  const handleBlur = (field: keyof typeof touched) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
    // 이메일 필드 blur 시 중복 확인 실행
    if (field === "email" && isEmailValid(formData.email)) {
      handleEmailCheck(formData.email);
    }
  };

  // 포커스 진입 시 touched 초기화 — 수정 중에는 에러 메시지 숨김
  const handleFocus = (field: keyof typeof touched) => {
    setTouched((prev) => ({ ...prev, [field]: false }));
  };
 
  // ===== 폼 유효성 검사 =====
  const isFormValid =
    validations.emailValid &&
    emailCheck.status === "available" &&
    validations.passwordStrong &&
    validations.passwordMatch &&
    validations.nicknameValid &&
    validations.phoneValid;
 
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
 
    if (!isFormValid) {
      toast.error("모든 필드를 올바르게 입력해주세요");
      return;
    }
 
   
    signup.mutate(
      { 
        nickname: formData.nickname,
        password: formData.password,
        email: formData.email,
        phone: formData.phone,
      },
      {
        onSuccess: () => {
          toast.success("회원가입이 완료되었습니다.");
          navigate("/");
        },
        onError: (error) => {
          toast.error(resolveErrorMessage(error, "회원가입에 실패했습니다."));
          setFormData((prev) => ({
            ...prev,
            password: "",
          }));
        },
      }
    );
  };
 
  const passwordStrength = getPasswordStrength(formData.password);
 
  return (
    <div className="w-full max-w-md">
      {/* 로고 */}
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
          새로운 계정을 만들어 E스포츠 경기 티켓을 예매하세요
        </p>
      </div>
 
      {/* 폼 카드 */}
      <Card className="mt-8 space-y-5 p-8">
        {/* 서버 에러 메시지 */}
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
          {/* 이메일 필드 */}
          <div className="space-y-1.5">
            <Label htmlFor="email">
              이메일 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="email"
              name="email"            
              type="email"
              placeholder="example@email.com"
              value={formData.email}
              onChange={handleChange} 
              onFocus={() => handleFocus("email")}
              onBlur={() => handleBlur("email")}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  handleEmailCheck(formData.email);
                }
              }}
              disabled={signup.isPending}
              className="h-10"
            />
            {touched.email && formData.email && !isEmailValid(formData.email) && (
              <p className="mt-1 text-xs font-medium text-red-500">
                올바른 이메일 형식이 아닙니다.
              </p>
            )}
            {touched.email && validations.emailValid && emailCheck.status === "checking" && (
              <p className="mt-1 flex items-center gap-1 text-xs font-medium text-muted-foreground">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                이메일 중복 확인 중...
              </p>
            )}
            {touched.email && emailCheck.status === "available" && (
              <p className="mt-1 flex items-center gap-1 text-xs font-medium text-green-600">
                <CheckCircle2 className="h-3.5 w-3.5" />
                사용 가능한 이메일입니다
              </p>
            )}
            {touched.email && emailCheck.status === "duplicate" && (
              <p className="mt-1 flex items-center gap-1 text-xs font-medium text-red-500">
                <AlertCircle className="h-3.5 w-3.5" />
                이미 사용 중인 이메일입니다
              </p>
            )}
          </div>
 
          {/* 이름 필드 */}
          <div className="space-y-1.5">
            <Label htmlFor="nickname">
              이름 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="nickname"
              name="nickname"             
              type="text"
              placeholder="2자 이상 20자 이하"
              value={formData.nickname}
              onChange={handleChange} 
              onFocus={() => handleFocus("nickname")}
              onBlur={() => handleBlur("nickname")}
              disabled={signup.isPending}
              className="h-10"
            />
            {touched.nickname && formData.nickname && !isNicknameValid(formData.nickname) && (
              <p className="mt-1 text-xs font-medium text-red-500">
                이름은 2자 이상 20자 이하이어야 합니다. (현재: {formData.nickname.length}자)
              </p>
            )}
          </div>
 
          {/* 전화번호 필드 */}
          <div className="space-y-1.5">
            <Label htmlFor="phone">전화번호</Label>
            <Input
              id="phone"
              name="phone"          
              type="tel"
              placeholder="010-1234-5678"
              value={formData.phone}
              onChange={handleChange} 
              onFocus={() => handleFocus("phone")}
              onBlur={() => handleBlur("phone")}
              disabled={signup.isPending}
              className="h-10"
            />
            {touched.phone && formData.phone && !isPhoneValid(formData.phone) && (
              <p className="mt-1 text-xs font-medium text-red-500">
                전화번호 형식은 010-XXXX-XXXX 이어야 합니다.
              </p>
            )}
          </div>
 
          {/* 비밀번호 필드 */}
          <div className="space-y-1.5">
            <Label htmlFor="password">
              비밀번호 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="password"
              name="password"       
              type="password"
              placeholder="8-20자, 영문+숫자+특수문자 필수"
              value={formData.password}
              onChange={handleChange} 
              onFocus={() => handleFocus("password")}
              onBlur={() => handleBlur("password")}
              disabled={signup.isPending}
              className="h-10"
            />
            {touched.password && formData.password && !isPasswordStrong(formData.password) && (
              <p className="mt-1 text-xs font-medium text-red-500">
                비밀번호는 8자 이상 20자 이하이며, 영문+숫자+특수문자를 포함해야 합니다.
              </p>
            )}
 
            {touched.password && formData.password && (
              <div className="mt-2 space-y-1 text-xs">
                <RequirementItem
                  met={passwordStrength.minLength}
                  text="최소 8자 이상"
                />
                <RequirementItem
                  met={passwordStrength.maxLength}
                  text="최대 20자 이하"
                />
                <RequirementItem
                  met={passwordStrength.hasLetter}
                  text="영문(대소문자) 포함"
                />
                <RequirementItem
                  met={passwordStrength.hasNumber}
                  text="숫자 포함"
                />
                <RequirementItem
                  met={passwordStrength.hasSpecial}
                  text="특수문자 포함 (@$!%*#?&)"
                />
              </div>
            )}
          </div>

          {/* 비밀번호 확인 필드 */}
          <div className="space-y-1.5">
            <Label htmlFor="passwordConfirm">
              비밀번호 확인 <span className="text-red-500">*</span>
            </Label>
            <Input
              id="passwordConfirm"
              name="passwordConfirm"  // ✅ 필수
              type="password"
              placeholder="비밀번호를 다시 입력해주세요"
              value={formData.passwordConfirm}
              onChange={handleChange} // ✅ 필수
              onFocus={() => handleFocus("passwordConfirm")}
              onBlur={() => handleBlur("passwordConfirm")}
              disabled={signup.isPending}
              className="h-10"
            />
            {touched.passwordConfirm &&
              formData.passwordConfirm &&
              !validations.passwordMatch && (
                <p className="mt-1 text-xs font-medium text-red-500">
                  비밀번호가 일치하지 않습니다
                </p>
              )}
          </div>


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
          <Link to="/login" className="font-semibold text-[#1C5EFD] hover:underline">
            로그인
          </Link>
        </div>
      </Card>
    </div>
  );
}
 
// ===== RequirementItem 컴포넌트 =====
 
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