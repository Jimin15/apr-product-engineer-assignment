import { describe, expect, it } from "vitest";
import { commentTime, formatDate, relativeTime } from "./time";

const NOW = Date.parse("2026-10-05T12:00:00Z");
const ago = (seconds: number) => new Date(NOW - seconds * 1000).toISOString();
const MIN = 60;
const HOUR = 3600;
const DAY = 86400;

describe("relativeTime (F-12)", () => {
  it.each([
    [0, "방금 전"],
    [59, "방금 전"],
    [60, "1분 전"],
    [59 * MIN + 59, "59분 전"],
    [60 * MIN, "1시간 전"],
    [23 * HOUR, "23시간 전"],
    [24 * HOUR, "1일 전"],
    [6 * DAY, "6일 전"],
    [7 * DAY, "1주 전"],
    [29 * DAY, "4주 전"],
    [30 * DAY, "1개월 전"],
    [364 * DAY, "12개월 전"],
    [365 * DAY, "1년 전"],
    [800 * DAY, "2년 전"],
  ])("%d초 전 → %s", (seconds, expected) => {
    expect(relativeTime(ago(seconds), NOW)).toBe(expected);
  });

  it("미래 시각은 방금 전", () => {
    expect(relativeTime(ago(-120), NOW)).toBe("방금 전");
  });
});

describe("formatDate (F-20 · plan F7)", () => {
  it("한국 시간 기준 YY.MM.DD — UTC 날짜가 바뀌는 경계", () => {
    expect(formatDate("2026-08-30T15:30:00Z")).toBe("26.08.31");
    expect(formatDate("2026-08-30T14:59:59Z")).toBe("26.08.30");
  });

  it("공백 · 끝 마침표가 없다", () => {
    expect(formatDate("2025-08-08T00:00:00Z")).toMatch(/^\d{2}\.\d{2}\.\d{2}$/);
  });
});

describe("commentTime (F-33)", () => {
  it("7일 미만은 상대 시간", () => {
    expect(commentTime(ago(13 * HOUR), NOW)).toBe("13시간 전");
    expect(commentTime(ago(7 * DAY - 1), NOW)).toBe("6일 전");
  });

  it("7일 이상은 절대 날짜", () => {
    expect(commentTime(ago(7 * DAY), NOW)).toMatch(/^\d{2}\.\d{2}\.\d{2}$/);
  });
});
