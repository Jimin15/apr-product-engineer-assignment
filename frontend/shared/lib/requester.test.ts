import { describe, expect, it } from "vitest";
import { DEFAULT_REQUESTER, isOwner, parseRequester } from "./requester";

describe("parseRequester (F-04 · B-01 과 같은 규칙)", () => {
  it.each([
    ["", DEFAULT_REQUESTER],
    ["other=1", DEFAULT_REQUESTER],
    ["x-user-id=", DEFAULT_REQUESTER],
    ["x-user-id=%20%20", DEFAULT_REQUESTER],
    ["x-user-id=Jelin", "Jelin"],
    ["a=1; x-user-id=Roidl_08; b=2", "Roidl_08"],
    ["x-user-id=%20cos_holic%20", "cos_holic"],
  ])("%j → %s", (cookie, expected) => {
    expect(parseRequester(cookie)).toBe(expected);
  });

  it("디코딩할 수 없는 값은 그대로 쓴다", () => {
    expect(parseRequester("x-user-id=%E0%A4%A")).toBe("%E0%A4%A");
  });
});

describe("isOwner (P-02)", () => {
  it("대소문자를 구분해 정확히 일치해야 한다", () => {
    expect(isOwner("Jelin", "Jelin")).toBe(true);
    expect(isOwner("Jelin", "jelin")).toBe(false);
    expect(isOwner("Jelin", "Jelin ")).toBe(false);
  });
});
