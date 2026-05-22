// 백엔드 시드의 thumbnailUrl / detailImageUrl 키 → 로컬 이미지 매핑
// 시연용 — 추후 어드민이 실제 이미지 URL을 업로드하면 키 매칭에서 빠지고
// http(s)://로 시작하는 URL은 resolver 가 그대로 통과시킴
import lckThumb from "@/image/Lck 결승전.png";

const IMAGE_MAP: Record<string, string> = {
  "lck_thumb.png": lckThumb,
  "lck_detail.png": lckThumb,
};

/**
 * 백엔드 이미지 키 또는 URL → 실제 src 값
 *  - http(s)://로 시작하면 그대로 반환 (실제 URL 가정)
 *  - 그 외에는 IMAGE_MAP lookup
 *  - 매핑 없으면 null (호출자가 placeholder 처리)
 */
export function resolveEventImage(
  url: string | null | undefined,
): string | null {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return IMAGE_MAP[url] ?? null;
}
