export const formatPrice = (won: number) =>
  `${won.toLocaleString("ko-KR")}원`;

export const formatPriceShort = (won: number) => {
  if (won >= 100_000_000) return `₩${(won / 100_000_000).toFixed(0)}억`;
  if (won >= 10_000_000) return `₩${(won / 1_000_000).toFixed(0)}M`;
  return formatPrice(won);
};

export const formatDate = (iso: string, withTime = false) => {
  const d = new Date(iso);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  if (!withTime) return `${yyyy}.${mm}.${dd}`;
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd} ${hh}:${mi}`;
};

export const formatDateRange = (from: string, to: string) =>
  `${formatDate(from)} ~ ${formatDate(to)}`;

const KOR_DAYS = ["일", "월", "화", "수", "목", "금", "토"] as const;

export const formatShortDate = (iso: string) => {
  const d = new Date(iso);
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${mm}.${dd} (${KOR_DAYS[d.getDay()]})`;
};

export const formatTime = (iso: string) => {
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2, "0")}:${String(
    d.getMinutes(),
  ).padStart(2, "0")}`;
};

/**
 * colorHex 정규화: "#" 없으면 붙임
 * 백엔드 시드 데이터가 "FF0000" 형태로 저장되어 있을 수 있음
 */
export const normalizeColorHex = (hex: string | null | undefined): string => {
  if (!hex) return "#6b7280";
  return hex.startsWith("#") ? hex : `#${hex}`;
};
