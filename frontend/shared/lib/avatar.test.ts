import { describe, expect, it } from "vitest";
import { avatarIndex, avatarSrc } from "./avatar";

describe("avatar (F-07)", () => {
  it("같은 작성자는 항상 같은 아바타", () => {
    expect(avatarSrc("Jelin")).toBe(avatarSrc("Jelin"));
  });

  it("결과는 1~3", () => {
    for (const author of ["Jelin", "Roidl_08", "apr_tester", "베리베리", ""]) {
      expect([1, 2, 3]).toContain(avatarIndex(author));
      expect(avatarSrc(author)).toMatch(/^\/avatar-[123]\.png$/);
    }
  });
});
