import { describe, expect, it, vi } from "vitest";
import { goBack } from "./goBack";

const makeRouter = () => ({ back: vi.fn(), push: vi.fn(), replace: vi.fn() });

describe("goBack (F-08)", () => {
  it("앱 안에서 이동해 온 기록이 있으면 뒤로", () => {
    const router = makeRouter();
    goBack(router, true);
    expect(router.back).toHaveBeenCalledOnce();
    expect(router.push).not.toHaveBeenCalled();
  });

  it("주소로 직접 들어와 기록이 없으면 목록으로", () => {
    const router = makeRouter();
    goBack(router, false);
    expect(router.push).toHaveBeenCalledWith("/");
    expect(router.back).not.toHaveBeenCalled();
  });
});
