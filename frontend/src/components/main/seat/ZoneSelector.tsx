import { Button } from "@/components/ui/button";
import stadia from "@/image/stadium-bg.png"; // 경기장 배경 이미지 경로 수정
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
        className="w-full max-w-5xl h-96 rounded-3xl bg-cover bg-center relative shadow-2xl"
        style={{
          backgroundImage: `url(${stadia})`,
        }}
      >
        {/* 어두운 오버레이 */}
        <div className="absolute inset-0 bg-black/20 rounded-3xl"></div>
         {/* 좌측 스크린 */}
 
        {/* 구역 버튼들 - 배경에 맞춰 배치 */}
        <div className="absolute inset-0 flex items-center justify-between px-6 rounded-3xl">
          {/* 좌측 구역 (홈) */}
          {homeSection && (
            <Button
              onClick={() => onSelect(homeSection.sectionId)}
              className="absolute
              left-[40px]
              top-[40%]
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-40 h-50
              bg-blue-500 hover:bg-blue-600
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="text-sm font-semibold text-white/90">BLUE HOME</span>
              <span className="text-2xl font-bold">{sectionLabel(homeSection.name)}</span>
            </Button>
          )}
 
          {/* 중앙 구역 (스테이지) */}
          {stageSection && (
            <Button
              onClick={() => onSelect(stageSection.sectionId)}
              className=" absolute
              left-[609px]
              top-[87%]
              -translate-x-2/2
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-50 h-25
              bg-purple-600 hover:bg-purple-700
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="text-sm font-semibold text-white/90">STAGE</span>
              <span className="text-2xl font-bold">{sectionLabel(stageSection.name)}</span>
            </Button>
          )}
          {/* 중앙 구역 (스테이지) */}
          {stageSection2 && (
            <Button
              onClick={() => onSelect(stageSection2.sectionId)}
              className=" absolute
              left-[170px]
              top-[87%]
              -translate-x-0/20
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-50 h-25
              bg-purple-600 hover:bg-purple-700
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="text-sm font-semibold text-white/90">STAGE</span>
              <span className="text-2xl font-bold">{sectionLabel(stageSection2.name)}</span>
            </Button>
          )}
 
          {/* 우측 구역 (원정) */}
          {awaySection && (
            <Button
              onClick={() => onSelect(awaySection.sectionId)}
              className="absolute
              right-[35px]
              top-[40%]
              -translate-y-1/2

              flex flex-col items-center justify-center
              w-40 h-50
              bg-red-500 hover:bg-red-600
              text-white text-center
              rounded-2xl shadow-xl
              hover:scale-110 active:scale-95
              transition-all duration-300"
            >
              <span className="text-sm font-semibold text-white/90">RED AWAY</span>
              <span className="text-2xl font-bold">{sectionLabel(awaySection.name)}</span>
            </Button>
          )}
        </div>
      </div>
      {/* 범례 */}
      <div className="flex flex-wrap justify-center gap-8 mt-6 p-6 bg-gradient-to-r from-blue-50 to-red-50 rounded-xl w-full max-w-5xl border border-gray-200">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-blue-500 shadow-md"></div>
          <div>
            <div className="text-sm font-bold text-gray-900">A</div>
            <div className="text-xs text-gray-600">좌측</div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-purple-600 shadow-md"></div>
          <div>
            <div className="text-sm font-bold text-gray-900">B</div>
            <div className="text-xs text-gray-600">중앙</div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-purple-600 shadow-md"></div>
          <div>
            <div className="text-sm font-bold text-gray-900">C</div>
            <div className="text-xs text-gray-600">중앙</div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-red-500 shadow-md"></div>
          <div>
            <div className="text-sm font-bold text-gray-900">D</div>
            <div className="text-xs text-gray-600">우측</div>
          </div>
        </div>
      </div>
    </div>
  );
}
 
const sectionLabel = (name: string) => name.replace(/\s?구역$/, "");