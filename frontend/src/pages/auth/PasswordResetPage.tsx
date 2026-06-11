import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Lock, ShieldCheck, ShieldX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { confirmPasswordReset } from "@/api/auth";
import { resolveErrorMessage } from "@/lib/error";

// 백엔드 @Pattern: 8~20자, 영문+숫자+특수문자(@$!%*#?&) 필수
const PASSWORD_PATTERN =
  /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;

type PageState = "FORM" | "DONE" | "INVALID";

export default function PasswordResetPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get("token") ?? "";

  const [pageState, setPageState] = useState<PageState>("FORM");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [loading, setLoading] = useState(false);

  // 토큰이 없으면 바로 INVALID 처리
  useEffect(() => {
    if (!token) setPageState("INVALID");
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!PASSWORD_PATTERN.test(password)) {
      return toast.error(
        "비밀번호는 8~20자이며, 영문·숫자·특수문자(@$!%*#?&)를 모두 포함해야 합니다."
      );
    }
    if (password !== passwordConfirm) {
      return toast.error("비밀번호가 일치하지 않습니다.");
    }

    setLoading(true);
    try {
      await confirmPasswordReset(token, password);
      setPageState("DONE");
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response
        ?.status;
      if (status === 400 || status === 404) {
        // 토큰 만료 or 유효하지 않음
        setPageState("INVALID");
      } else {
        toast.error(resolveErrorMessage(err, "비밀번호 변경에 실패했습니다."));
      }
    } finally {
      setLoading(false);
    }
  };

  // 완료 화면
  if (pageState === "DONE") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
        <div className="w-full max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-sm text-center">
          <div className="mx-auto inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600">
            <ShieldCheck className="h-8 w-8" />
          </div>
          <div>
            <h2 className="text-xl font-bold">비밀번호 변경 완료</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              새 비밀번호로 로그인해주세요.
            </p>
          </div>
          <Button
            size="lg"
            onClick={() => navigate("/login")}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            로그인 하러 가기
          </Button>
        </div>
      </div>
    );
  }

  // 토큰 무효 / 만료 화면
  if (pageState === "INVALID") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
        <div className="w-full max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-sm text-center">
          <div className="mx-auto inline-flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-red-500">
            <ShieldX className="h-8 w-8" />
          </div>
          <div>
            <h2 className="text-xl font-bold">링크가 유효하지 않습니다</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              재설정 링크가 만료되었거나 이미 사용된 링크입니다.
              <br />
              로그인 화면에서 다시 요청해주세요.
            </p>
          </div>
          <Button
            size="lg"
            onClick={() => navigate("/login")}
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            로그인 화면으로 이동
          </Button>
        </div>
      </div>
    );
  }

  // 비밀번호 재설정 폼
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md space-y-6 rounded-2xl bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-xl font-bold">새 비밀번호 설정</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            사용할 새 비밀번호를 입력해주세요.
          </p>
        </div>

        <form className="space-y-4" onSubmit={handleSubmit}>
          {/* 새 비밀번호 */}
          <div className="space-y-2">
            <Label htmlFor="pr-pw">새 비밀번호</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="pr-pw"
                type="password"
                placeholder="영문, 숫자, 특수문자 포함 8~20자"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pl-10"
                required
              />
            </div>
          </div>

          {/* 새 비밀번호 확인 */}
          <div className="space-y-2">
            <Label htmlFor="pr-pw-confirm">새 비밀번호 확인</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="pr-pw-confirm"
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

          {/* 비밀번호 조건 안내 */}
          <div className="rounded-lg bg-blue-50 p-4 text-sm">
            <h4 className="font-semibold">비밀번호 설정 조건</h4>
            <ul className="mt-2 list-disc space-y-1 pl-5 text-muted-foreground">
              <li>8자 이상 20자 이하</li>
              <li>영문, 숫자, 특수문자(@$!%*#?&)를 모두 포함</li>
            </ul>
          </div>

          <Button
            type="submit"
            size="lg"
            disabled={
              loading ||
              (!!passwordConfirm && password !== passwordConfirm)
            }
            className="w-full bg-[#1C5EFD] hover:bg-[#316DFD]"
          >
            {loading ? "변경 중..." : "비밀번호 변경"}
          </Button>
        </form>
      </div>
    </div>
  );
}
