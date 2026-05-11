import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useEventFilterStore } from "@/store";

export function HeroSection() {
  const keyword = useEventFilterStore((s) => s.keyword);
  const setKeyword = useEventFilterStore((s) => s.setKeyword);

  return (
    <section className="bg-gradient-to-br from-orange-50 via-white to-orange-50 px-6 py-20">
      <div className="mx-auto max-w-7xl">
        <h2 className="text-4xl font-bold leading-tight md:text-5xl">
          최고의 E스포츠 경기를
          <br />
          바로 예매하세요
        </h2>
        <p className="mt-4 text-muted-foreground">
          LOL, 발로란트, 오버워치 등 모든 E스포츠 경기 티켓을 간편하게 예매할
          수 있습니다
        </p>

        <div className="mt-8 flex max-w-2xl gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="게임명, 팀명, 선수명으로 검색하세요"
              className="h-12 pl-10"
            />
          </div>
          <Button
            size="lg"
            className="h-12 bg-[#FF6B47] px-8 text-base hover:bg-[#E5532E]"
          >
            검색
          </Button>
        </div>
      </div>
    </section>
  );
}
