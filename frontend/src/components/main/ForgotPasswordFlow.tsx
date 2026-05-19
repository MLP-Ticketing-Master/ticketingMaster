import { useState } from "react";
import { ArrowLeft, KeyRound, Lock, Mail, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";

type Step = "EMAIL" | "CODE" | "RESET" | "DONE";

interface Props {
  onBack: () => void;
}

// TODO: 실제 API 연동 시 교체
async function mockSendCode(email: string) {
  await new Promise((r) => setTimeout(r, 800));
  console.log("[mock] 인증코드 발송 →", email);
}
async function mockVerifyCode(_code: string) {
  await new Promise((r) => setTimeout(r, 600));
  return true; // 항상 성공 (mock)
}
async function mockResetPassword(_password: string) {
  await new Promise((r) => setTimeout(r, 800));
}

export function ForgotPasswordFlow({ onBack }: Props) {
  const [step, setStep] = useState<Step>("EMAIL");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [loading, setLoading] = useState(false);

  // ── STEP 1: 이메일 입력 → 인증코드 발송 ──────────────────────────
  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return toast.error("이메일을 입력해주세요.");
    setLoading(true);
    try {
      await mockSendCode(email);
      toast.success("인증코드가 이메일로 발송되었습니다.");
      setStep("CODE");
    } catch {
      toast.error("인증코드 발송에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  // ── STEP 2: 인증코드 확인 ─────────────────────────────────────────
  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (code.length < 4) return toast.error("인증코드를 입력해주세요.");
    setLoading(true);
    try {
      const ok = await mockVerifyCode(code);
      if (!ok) return toast.error("인증코드가 올바르지 않습니다.");
      setStep("RESET");
    } catch {
      toast.error("인증 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ── STEP 3: 새 비밀번호 설정 ─────────────────────────────────────
  const handleReset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 8) return toast.error("비밀번호는 8자 이상이어야 합니다.");
    if (password !== passwordConfirm) return toast.error("비밀번호가 일치하지 않습니다.");
    setLoading(true);
    try {
      await mockResetPassword(password);
      setStep("DONE");
    } catch {
      toast.error("비밀번호 변경에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ── DONE ──────────────────────────────────────────────────────────
  if (step === "DONE") {
    return (
      <div className="space-y-6 text-center">
        <div className="mx-auto inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600">
          <ShieldCheck className="h-8 w-8" />
        </div>
        <div>
          <h2 className="text-lg font-bold">비밀번호 변경 완료</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            새 비밀번호로 로그인해주세요.
          </p>
        </div>
        <Button
          size="lg"
          onClick={onBack}
          className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
        >
          로그인 화면으로 돌아가기
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={step === "EMAIL" ? onBack : () => setStep(step === "CODE" ? "EMAIL" : "CODE")}
          className="text-muted-foreground hover:text-foreground transition-colors"
          aria-label="뒤로가기"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h2 className="text-base font-bold">비밀번호 찾기</h2>
          <p className="text-xs text-muted-foreground">
            {step === "EMAIL" && "가입한 이메일 주소를 입력해주세요."}
            {step === "CODE" && `${email}로 발송된 인증코드를 입력해주세요.`}
            {step === "RESET" && "새로운 비밀번호를 설정해주세요."}
          </p>
        </div>
      </div>

      {/* 스텝 인디케이터 */}
      <div className="flex items-center gap-1">
        {(["EMAIL", "CODE", "RESET"] as Step[]).map((s, i) => (
          <div key={s} className="flex flex-1 items-center gap-1">
            <div
              className={`h-1.5 flex-1 rounded-full transition-colors ${
                ["EMAIL", "CODE", "RESET"].indexOf(step) >= i
                  ? "bg-[#054EFD]"
                  : "bg-gray-200"
              }`}
            />
          </div>
        ))}
      </div>

      {/* ── STEP 1: 이메일 ── */}
      {step === "EMAIL" && (
        <form className="space-y-4" onSubmit={handleSendCode}>
          <div className="space-y-2">
            <Label htmlFor="fp-email">이메일</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="fp-email"
                type="email"
                placeholder="가입한 이메일 주소"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="pl-10"
                required
              />
            </div>
          </div>
          <Button
            type="submit"
            size="lg"
            disabled={loading}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {loading ? "발송 중..." : "인증코드 발송"}
          </Button>
        </form>
      )}

      {/* ── STEP 2: 인증코드 ── */}
      {step === "CODE" && (
        <form className="space-y-4" onSubmit={handleVerifyCode}>
          <div className="space-y-2">
            <Label htmlFor="fp-code">인증코드</Label>
            <div className="relative">
              <KeyRound className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="fp-code"
                type="text"
                inputMode="numeric"
                placeholder="인증코드 6자리"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                className="pl-10 tracking-widest"
                required
              />
            </div>
          </div>
          <Button
            type="submit"
            size="lg"
            disabled={loading}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {loading ? "확인 중..." : "인증코드 확인"}
          </Button>
          <button
            type="button"
            onClick={() => { setCode(""); handleSendCode(new Event("click") as any); }}
            className="w-full text-center text-xs text-muted-foreground underline underline-offset-2 hover:text-foreground"
          >
            인증코드 재발송
          </button>
        </form>
      )}

      {/* ── STEP 3: 새 비밀번호 ── */}
      {step === "RESET" && (
        <form className="space-y-4" onSubmit={handleReset}>
          <div className="space-y-2">
            <Label htmlFor="fp-pw">새 비밀번호</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="fp-pw"
                type="password"
                placeholder="8자 이상"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pl-10"
                required
              />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="fp-pw-confirm">새 비밀번호 확인</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="fp-pw-confirm"
                type="password"
                placeholder="비밀번호를 다시 입력하세요"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                className={`pl-10 ${
                  passwordConfirm && password !== passwordConfirm
                    ? "border-red-400 focus-visible:ring-red-400"
                    : ""
                }`}
                required
              />
            </div>
            {passwordConfirm && password !== passwordConfirm && (
              <p className="text-xs text-red-500">비밀번호가 일치하지 않습니다.</p>
            )}
          </div>
          <Button
            type="submit"
            size="lg"
            disabled={loading || (!!passwordConfirm && password !== passwordConfirm)}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {loading ? "변경 중..." : "비밀번호 변경"}
          </Button>
        </form>
      )}
    </div>
  );
}
