const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/** 목록 상대 시간 (F-12 · FD-2). 미래 시각은 `방금 전`. */
export function relativeTime(iso: string, now: number = Date.now()): string {
  const seconds = Math.floor((now - Date.parse(iso)) / 1000);
  if (seconds < MINUTE) return "방금 전";
  if (seconds < HOUR) return `${Math.floor(seconds / MINUTE)}분 전`;
  if (seconds < DAY) return `${Math.floor(seconds / HOUR)}시간 전`;
  const days = Math.floor(seconds / DAY);
  if (days < 7) return `${days}일 전`;
  if (days < 30) return `${Math.floor(days / 7)}주 전`;
  if (days < 365) return `${Math.floor(days / 30)}개월 전`;
  return `${Math.floor(days / 365)}년 전`;
}

const seoulDate = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "2-digit",
  month: "2-digit",
  day: "2-digit",
});

/** 절대 날짜 `YY.MM.DD`, 한국 시간 기준 (F-20 · plan F7). */
export function formatDate(iso: string): string {
  const parts = seoulDate.formatToParts(new Date(iso));
  const pick = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? "";
  return `${pick("year")}.${pick("month")}.${pick("day")}`;
}

/** 댓글 시각: 7일 미만 상대 · 7일 이상 절대 (F-33 · FD-3). */
export function commentTime(iso: string, now: number = Date.now()): string {
  return now - Date.parse(iso) < 7 * DAY * 1000
    ? relativeTime(iso, now)
    : formatDate(iso);
}
