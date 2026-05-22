import { useState } from "react";
import { ArrowLeft, Mail, MailCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { requestPasswordReset } from "@/api/auth.api";

type Step = "EMAIL" | "SENT";

interface Props {
  onBack: () => void;
}

export function ForgotPasswordFlow({ onBack }: Props) {
  const [step, setStep] = useState<Step>("EMAIL");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  // 이메일 입력 → 재설정 링크 발송 요청
  const handleSendLink = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return toast.error("이메일을 입력해주세요.");
    setLoading(true);
    try {
      await requestPasswordReset(email);
      setStep("SENT");
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "이메일 발송에 실패했습니다. 다시 시도해주세요.";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // 발송 완료 화면
  if (step === "SENT") {
    return (
      <div className="space-y-6 text-center">
        <div className="mx-auto inline-flex h-16 w-16 items-center justify-center rounded-full bg-blue-100 text-blue-600">
          <MailCheck className="h-8 w-8" />
        </div>
        <div>
          <h2 className="text-lg font-bold">이메일을 확인해주세요</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            <span className="font-medium text-foreground">{email}</span>
            <br />
            으로 비밀번호 재설정 링크를 발송했습니다.
          </p>
          <p className="mt-2 text-xs text-muted-foreground">
            링크는 발송 후 30분간 유효합니다.
          </p>
        </div>

        <div className="space-y-2">
          <Button
            size="lg"
            onClick={onBack}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            로그인 화면으로 돌아가기
          </Button>
          <button
            type="button"
            onClick={() => {
              setStep("EMAIL");
              setEmail("");
            }}
            className="w-full text-center text-xs text-muted-foreground underline underline-offset-2 hover:text-foreground"
          >
            다른 이메일로 재시도
          </button>
        </div>
      </div>
    );
  }

  // 이메일 입력 화면
  return (
    <div className="space-y-5">
      {/* 헤더 */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onBack}
          className="text-muted-foreground hover:text-foreground transition-colors"
          aria-label="뒤로가기"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h2 className="text-base font-bold">비밀번호 찾기</h2>
          <p className="text-xs text-muted-foreground">
            가입한 이메일 주소를 입력하면 재설정 링크를 보내드립니다.
          </p>
        </div>
      </div>

      <form className="space-y-4" onSubmit={handleSendLink}>
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
          {loading ? "발송 중..." : "재설정 링크 발송"}
        </Button>
      </form>
    </div>
  );
}
