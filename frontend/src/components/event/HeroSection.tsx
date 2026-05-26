import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useEventFilterStore } from "@/store";
import backgroundImage from "@/assets/background.png"

export function HeroSection() {
  const keyword = useEventFilterStore((s) => s.keyword);
  const setKeyword = useEventFilterStore((s) => s.setKeyword);
  


  return (
    <section className="relative px-6 py-28 overflow-hidden group">
      <div
      className="absolute inset-0 bg-cover bg-center transition-transform duration-[3s] ease-out group-hover:scale-105"
      style={{
        backgroundImage: `url(${backgroundImage})`,
      }}
    /> 
    
    <div className="absolute inset-0 bg-gradient-to-br from-gray-900/90 via-black/50 to-gray-800/90 " />
    
     <div className="absolute -top-32 -right-32 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />
      <div className="absolute -bottom-32 -left-32 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl" />

      <div className="mx-auto max-w-7xl relative z-10">
        <h2 className="text-5xl md:text-6xl font-bold leading-tight text-white animate-fade-in-up animation-delay-100">
          광클 없이,

          <br />
           <span className="bg-gradient-to-r from-blue-400 via-blue-500 to-cyan-400 bg-clip-text text-transparent">
          원하는 자리를 빠르게!
          </span>
        </h2>
        <p className="mt-4 text-muted-foreground text-white animate-fade-in-up animation-delay-100">
          LOL, 발로란트, 오버워치 등 모든 E스포츠 경기 티켓을 간편하게 예매할
          수 있습니다
        </p>

        <div className="mt-8 flex flex-wrap max-w-2xl gap-2 text-white animate-fade-in-up animation-delay-100">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground text-white" />
            <Input
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="게임명, 팀명, 선수명으로 검색하세요"
              className="h-12 pl-10 placeholder:text-gray-400 hover:placeholder:text-[#FFFFFF]"
            />
          </div>
          
          <Button
            size="lg"
            className="h-12 bg-[#054EFD] px-8 text-base hover:bg-[#316DFD]"
          >
            검색
          </Button>
        </div>
        
      </div>
       <style>{`
        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .animate-fade-in-up {
          animation: fadeInUp 0.8s ease-out forwards;
          opacity: 0;
        }

        .animation-delay-100 {
          animation-delay: 0.1s;
        }

        .animation-delay-200 {
          animation-delay: 0.2s;
        }

        .animation-delay-300 {
          animation-delay: 0.3s;
        }
      `}</style>
    </section>
  );
}
