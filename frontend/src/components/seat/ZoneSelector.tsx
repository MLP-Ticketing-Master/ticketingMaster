import { Button } from "@/components/ui/button";
import stadia from "@/assets/stadium-bg.png"; // 경기장 배경 이미지 경로 수정
import type { SectionItem } from "@/types";

interface Props {
  sections: SectionItem[];
  onSelect: (sectionId: number) => void;
}
 
export function ZoneSelector({ sections, onSelect }: Props) {
  const ordered = [...sections].sort(
    (a, b) => a.displayOrder - b.displayOrder,
  );
 
  // 4개 구역으로 분할
  const [homeSection, stageSection, stageSection2,awaySection] = ordered.length >= 4
    ? [ordered[0], ordered[2], ordered[1], ordered[3]]
    :  [
          ordered.at(0) ?? null,      // 첫 번째 또는 null
          undefined,                  // 없음
          ordered.at(1) ?? ordered.at(0) ?? null,  // 두 번째 또는 첫 번째
          ordered.at(2) ?? ordered.at(0) ?? null,  // 세 번째 또는 첫 번째
        ];
 
  return (
    <div className="flex flex-col items-center gap-6 py-10 px-6">
      {/* 배경 이미지가 있는 경기장 */}
      <div
        className="relative
                    w-full
                    max-w-5xl
                    aspect-[16/9]
                    min-h-[180px]
                    sm:min-h-[280px]
                    rounded-3xl
                    bg-cover
                    bg-center
                    shadow-2xl
                    overflow-hidden"
        style={{
          backgroundImage: `url(${stadia})`,
        }}
      >
        {/* 어두운 오버레이 */}
        <div className="absolute inset-0 bg-black/20"></div>
         {/* 좌측 스크린 */}
 
        {/* 구역 버튼들 - 배경에 맞춰 배치 */}
        <div className="absolute inset-0">
          {/* 좌측 구역 (홈) */}
          {homeSection && (
            <Button
              onClick={() => onSelect(homeSection.sectionId)}
              className="absolute
              left-[4%]
              top-[40%]
              -translate-y-1/2

              flex flex-col items-center justify-center

              w-20 h-28
              sm:w-28 sm:h-36
              lg:w-40 lg:h-50
              bg-blue-500 hover:bg-blue-600
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="hidden sm:block text-xs lg:text-sm font-semibold text-white/90">BLUE HOME</span>
              <span className="text-sm sm:text-lg lg:text-2xl font-bold">{sectionLabel(homeSection.name)}</span>
            </Button>
          )}
 
          {/* 중앙 구역 (스테이지) */}
          {stageSection && (
            <Button
              onClick={() => onSelect(stageSection.sectionId)}
              className=" absolute
              left-[78%]
              top-[85%]
              -translate-x-full
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-24 h-12
              sm:w-32 sm:h-16
              lg:w-48 lg:h-24
              bg-purple-600 hover:bg-purple-700
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="hidden sm:block text-xs lg:text-sm font-semibold text-white/90">STAGE</span>
              <span className="text-xs sm:text-lg lg:text-2xl font-bold">{sectionLabel(stageSection.name)}</span>
            </Button>
          )}
          {/* 중앙 구역 (스테이지) */}
          {stageSection2 && (
            <Button
              onClick={() => onSelect(stageSection2.sectionId)}
              className=" absolute
              left-[25%]
              top-[85%]
              -translate-x-0/20
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-24 h-12
              sm:w-32 sm:h-16
              lg:w-48 lg:h-24
              bg-emerald-500 hover:bg-emerald-600
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="hidden sm:block text-xs lg:text-sm font-semibold text-white/90">STAGE</span>
              <span className="text-xs sm:text-base lg:text-2xl font-bold">{sectionLabel(stageSection2.name)}</span>
            </Button>
          )}
 
          {/* 우측 구역 (원정) */}
          {awaySection && (
            <Button
              onClick={() => onSelect(awaySection.sectionId)}
              className="absolute
              right-[4%]
              top-[40%]
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-20 h-28
              sm:w-28 sm:h-36
              lg:w-40 lg:h-48


              bg-red-500 hover:bg-red-600
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="hidden sm:block text-xs lg:text-sm font-semibold text-white/90">RED AWAY</span>
              <span className="text-sm sm:text-lg lg:text-2xl font-bold">{sectionLabel(awaySection.name)}</span>
            </Button>
          )}
        </div>
      </div>
      {/* 범례 — 백엔드 구역명 그대로 표시 (displayOrder 순) */}
      <div className="w-full
          max-w-5xl
          rounded-xl
          border
          border-gray-200
          bg-gradient-to-r
          from-blue-50
          to-red-50

          p-3
          sm:p-6

          flex
          flex-wrap
          justify-center
          gap-3
          sm:gap-6
          lg:gap-8">
        {ordered.map((section, idx) => (
          <div key={section.sectionId} className="flex items-center gap-3">
            <div className={`w-8 h-8 sm:w-10 sm:h-10 lg:w-12 lg:h-12 rounded-xl shadow-md ${LEGEND_COLORS[idx] ?? "bg-gray-400"}`} />
            <div>
              <div className="text-xs sm:text-sm font-bold text-gray-900">{section.name}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
 
const sectionLabel = (name: string) => name.replace(/\s?구역$/, "");

// 범례 색상 — displayOrder 순서대로 매칭 (스타디움 버튼 색과 일치)
// 0: 좌측(blue), 1: 좌측 스테이지(lime), 2: 우측 스테이지(purple), 3: 우측(red)
const LEGEND_COLORS = ["bg-blue-500", "bg-emerald-500", "bg-purple-600", "bg-red-500"];