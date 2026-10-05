export const DEFAULT_REQUESTER = "apr_tester";
const COOKIE_NAME = "x-user-id";

/**
 * 쿠키 문자열에서 요청자를 읽는다. 서버 규칙(B-01)과 같다:
 * 앞뒤 공백 제거, 비었으면 `apr_tester`.
 */
export function parseRequester(cookie: string): string {
  const hit = cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(`${COOKIE_NAME}=`));
  if (!hit) return DEFAULT_REQUESTER;

  let raw = hit.slice(COOKIE_NAME.length + 1);
  try {
    raw = decodeURIComponent(raw);
  } catch {
    // 디코딩할 수 없는 값은 그대로 둔다.
  }
  const value = raw.trim();
  return value.length > 0 ? value : DEFAULT_REQUESTER;
}

/** 본인 여부 — 대소문자 구분, 정확히 일치 (P-02 · F-05) */
export function isOwner(author: string, requester: string): boolean {
  return author === requester;
}

/** 브라우저에서만 호출한다. 서버 렌더링 중에는 기본값을 돌려준다. */
export function getRequester(): string {
  if (typeof document === "undefined") return DEFAULT_REQUESTER;
  return parseRequester(document.cookie);
}
