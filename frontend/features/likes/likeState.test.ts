import { describe, expect, it, vi } from "vitest";
import type { LikeResult } from "@/shared/api/types";
import { convergeLike, displayedCount } from "./likeState";

describe("displayedCount (F-21)", () => {
  it.each([
    [{ isLiked: false, likeCount: 42 }, false, 42],
    [{ isLiked: false, likeCount: 42 }, true, 43],
    [{ isLiked: true, likeCount: 43 }, true, 43],
    [{ isLiked: true, likeCount: 43 }, false, 42],
  ])("confirmed %j · desired %s → %d", (confirmed, desired, expected) => {
    expect(displayedCount(confirmed, desired)).toBe(expected);
  });
});

/** 서버 흉내: 호출마다 상태를 뒤집는 토글 */
function fakeServer(initial: LikeResult) {
  let state = initial;
  const toggle = vi.fn(async () => {
    state = {
      isLiked: !state.isLiked,
      likeCount: state.likeCount + (state.isLiked ? -1 : 1),
    };
    return state;
  });
  return { toggle, get state() { return state; } };
}

describe("convergeLike (FD-4)", () => {
  it("한 번 누르면 요청 1회, 결과는 좋아요", async () => {
    const server = fakeServer({ isLiked: false, likeCount: 42 });
    const result = await convergeLike(server.toggle, () => true);
    expect(server.toggle).toHaveBeenCalledTimes(1);
    expect(result).toEqual({ isLiked: true, likeCount: 43 });
  });

  it("요청 중에 한 번 더 누르면(두 번 연타) 한 번 더 토글해 원래 상태로", async () => {
    const server = fakeServer({ isLiked: false, likeCount: 42 });
    let desired = true;
    const toggle = vi.fn(async () => {
      const result = await server.toggle();
      if (toggle.mock.calls.length === 1) desired = false; // 첫 요청이 진행되는 동안 다시 누름
      return result;
    });
    const result = await convergeLike(toggle, () => desired);
    expect(toggle).toHaveBeenCalledTimes(2);
    expect(result).toEqual({ isLiked: false, likeCount: 42 });
  });

  it("세 번 연타: 첫 응답 뒤 원하는 상태가 같으면 요청은 1회", async () => {
    // 누름(true) → 취소(false) → 누름(true) 가 첫 응답 전에 모두 일어나면 desired 는 true
    const server = fakeServer({ isLiked: false, likeCount: 42 });
    const result = await convergeLike(server.toggle, () => true);
    expect(server.toggle).toHaveBeenCalledTimes(1);
    expect(result.isLiked).toBe(true);
  });

  it("실패하면 예외가 그대로 올라온다 (호출자가 서버 값으로 복구)", async () => {
    const toggle = vi.fn(async () => {
      throw new Error("network");
    });
    await expect(convergeLike(toggle, () => true)).rejects.toThrow("network");
  });

  it("끝없이 엇갈려도 상한에서 멈춘다", async () => {
    const server = fakeServer({ isLiked: false, likeCount: 0 });
    // 항상 서버 상태와 반대를 원하는 비정상 상황
    await convergeLike(server.toggle, () => !server.state.isLiked);
    expect(server.toggle.mock.calls.length).toBeLessThanOrEqual(10);
  });
});
