// 백엔드 시드의 team.logoImageUrl 키 → 로컬 이미지 매핑
// 시연용 — 추후 어드민이 실제 이미지 URL을 업로드하면 키 매칭에서 빠지고
// http(s)://로 시작하는 URL은 resolver 가 그대로 통과시킴
import t1Logo from "@/assets/T1_logo.png";
import gengLogo from "@/assets/GenG_logo.png";

const IMAGE_MAP: Record<string, string> = {
  // T1 — 백엔드 SeedData: name.toLowerCase().replace('.', ' ') → "t1_logo.png"
  "t1_logo.png": t1Logo,
  // Gen.G — 동일 규칙 → "gen g_logo.png" (dot → space)
  "gen g_logo.png": gengLogo,
};

/**
 * 백엔드 팀 로고 키 또는 URL → 실제 src 값
 *  - http(s)://로 시작하면 그대로 반환 (실제 URL 가정)
 *  - 그 외에는 IMAGE_MAP lookup
 *  - 매핑 없으면 null (호출자가 placeholder 처리)
 */
export function resolveTeamLogo(
  url: string | null | undefined,
): string | null {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return IMAGE_MAP[url] ?? null;
}
