import { describe, expect, it } from "vitest";
import {
  TITLE_MAX,
  countCodePoints,
  formatCount,
  isBlank,
  truncateCodePoints,
} from "./text";

describe("countCodePoints (P-42 · D2)", () => {
  it("시드 6번 제목(🙏🏻 포함)은 19 코드포인트 — UTF-16 으로는 21", () => {
    const title = "탈모에 좋은 제품 공유해주세요 🙏🏻";
    expect(title.length).toBe(21);
    expect(countCodePoints(title)).toBe(19);
  });

  it("피부색 수식 이모지는 2로 센다", () => {
    expect(countCodePoints("🙏🏻")).toBe(2);
  });
});

describe("truncateCodePoints", () => {
  it("21자 → 20자", () => {
    const text = "가".repeat(21);
    expect(countCodePoints(truncateCodePoints(text, TITLE_MAX))).toBe(20);
  });

  it("20자 이하는 그대로", () => {
    expect(truncateCodePoints("abc", TITLE_MAX)).toBe("abc");
  });

  it("서로게이트 반쪽이 남지 않는다 (19자 + 😌 + 1자 → 19자 + 😌)", () => {
    const text = "가".repeat(19) + "😌" + "나";
    const cut = truncateCodePoints(text, TITLE_MAX);
    expect(cut).toBe("가".repeat(19) + "😌");
    expect(cut.includes("�")).toBe(false);
  });

  it("알려진 한계: 여러 코드포인트 이모지는 경계에서 잘릴 수 있다 (🙏🏻 → 🙏)", () => {
    const text = "가".repeat(19) + "🙏🏻";
    expect(truncateCodePoints(text, TITLE_MAX)).toBe("가".repeat(19) + "🙏");
  });
});

describe("isBlank (P-41)", () => {
  it.each(["", "   ", "\n\n", " \t\n "])("%j 는 빈 값", (text) => {
    expect(isBlank(text)).toBe(true);
  });

  it("글자가 하나라도 있으면 빈 값이 아니다", () => {
    expect(isBlank(" a ")).toBe(false);
  });
});

describe("formatCount (F-06)", () => {
  it("천 단위 구분", () => {
    expect(formatCount(1815)).toBe("1,815");
    expect(formatCount(294)).toBe("294");
  });
});
