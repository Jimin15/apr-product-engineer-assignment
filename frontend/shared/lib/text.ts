/** 제목 최대 길이 — 유니코드 코드포인트 기준 (P-42 · D2) */
export const TITLE_MAX = 20;

/** 코드포인트 수. UTF-16 `length` 와 달리 이모지를 한 글자(이상)로 센다. */
export function countCodePoints(text: string): number {
  return [...text].length;
}

/** 코드포인트 기준으로 자른다. 서로게이트 반쪽이 남지 않는다. */
export function truncateCodePoints(text: string, max: number): string {
  const points = [...text];
  return points.length <= max ? text : points.slice(0, max).join("");
}

/** 공백 · 줄바꿈만 있으면 빈 값 (P-41) */
export function isBlank(text: string): boolean {
  return text.trim().length === 0;
}

/** 천 단위 구분 기호 (F-06) */
export function formatCount(n: number): string {
  return n.toLocaleString("ko-KR");
}
