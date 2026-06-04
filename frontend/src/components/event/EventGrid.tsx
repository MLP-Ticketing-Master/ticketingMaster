import { ChevronLeft, ChevronRight } from "lucide-react";
import { EventCard } from "./EventCard";
import type { EventListResponse } from "@/types";

const PAGE_SIZE = 6;

interface Props {
  events: EventListResponse[];
  currentPage: number;
  onPageChange: (page: number) => void;
}

export function EventGrid({ events, currentPage, onPageChange }: Props) {
  if (events.length === 0) {
    return (
      <div className="rounded-2xl border bg-white py-20 text-center text-muted-foreground">
        진행 중인 대회가 없습니다.
      </div>
    );
  }

  const totalPages = Math.ceil(events.length / PAGE_SIZE);
  const safePage = Math.min(Math.max(currentPage, 1), totalPages);
  const startIdx = (safePage - 1) * PAGE_SIZE;
  const pageEvents = events.slice(startIdx, startIdx + PAGE_SIZE);

  // 페이지 번호 목록 생성 (최대 5개, 현재 페이지 중심)
  const getPageNumbers = () => {
    const delta = 2;
    const range: number[] = [];
    const rangeStart = Math.max(1, safePage - delta);
    const rangeEnd = Math.min(totalPages, safePage + delta);
    for (let i = rangeStart; i <= rangeEnd; i++) range.push(i);
    return range;
  };

  const pageNumbers = getPageNumbers();

  return (
    <div className="space-y-8">
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {pageEvents.map((event) => (
          <EventCard key={event.eventId} event={event} />
        ))}
      </div>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-1.5">
          {/* 이전 버튼 */}
          <button
            onClick={() => onPageChange(safePage - 1)}
            disabled={safePage === 1}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 bg-white text-gray-500 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="이전 페이지"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>

          {/* 첫 페이지 + 말줄임 */}
          {pageNumbers[0] > 1 && (
            <>
              <button
                onClick={() => onPageChange(1)}
                className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 bg-white text-sm text-gray-600 transition-colors hover:bg-gray-50"
              >
                1
              </button>
              {pageNumbers[0] > 2 && (
                <span className="flex h-9 w-9 items-center justify-center text-sm text-gray-400">
                  …
                </span>
              )}
            </>
          )}

          {/* 페이지 번호들 */}
          {pageNumbers.map((page) => (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              className={`flex h-9 w-9 items-center justify-center rounded-lg border text-sm font-medium transition-colors ${
                page === safePage
                  ? "border-[#FF6B47] bg-[#FF6B47] text-white shadow-sm"
                  : "border-gray-200 bg-white text-gray-600 hover:bg-gray-50"
              }`}
              aria-current={page === safePage ? "page" : undefined}
            >
              {page}
            </button>
          ))}

          {/* 마지막 페이지 + 말줄임 */}
          {pageNumbers[pageNumbers.length - 1] < totalPages && (
            <>
              {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
                <span className="flex h-9 w-9 items-center justify-center text-sm text-gray-400">
                  …
                </span>
              )}
              <button
                onClick={() => onPageChange(totalPages)}
                className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 bg-white text-sm text-gray-600 transition-colors hover:bg-gray-50"
              >
                {totalPages}
              </button>
            </>
          )}

          {/* 다음 버튼 */}
          <button
            onClick={() => onPageChange(safePage + 1)}
            disabled={safePage === totalPages}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-gray-200 bg-white text-gray-500 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            aria-label="다음 페이지"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
