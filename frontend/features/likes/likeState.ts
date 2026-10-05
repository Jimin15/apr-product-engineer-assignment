// 좋아요 낙관적 반영의 순수 로직 (F-21 · FD-4). React 와 무관하게 테스트한다.
import type { LikeResult } from "@/shared/api/types";

/**
 * 화면에 보여줄 숫자: 서버가 확인한 숫자에, 확인된 상태와 원하는 상태가
 * 다를 때만 ±1 한다. 그 사이 다른 사용자의 좋아요는 응답 숫자로 반영된다.
 */
export function displayedCount(confirmed: LikeResult, desired: boolean): number {
  if (desired === confirmed.isLiked) return confirmed.likeCount;
  return confirmed.likeCount + (desired ? 1 : -1);
}

const MAX_TOGGLES = 10; // 연타가 끝없이 이어질 때의 안전장치

/**
 * API 는 "설정"이 아닌 "토글"이라 요청을 하나씩 보내고, 응답이 원하는 상태와
 * 다르면 한 번 더 토글해 마지막으로 누른 상태로 수렴시킨다.
 */
export async function convergeLike(
  toggle: () => Promise<LikeResult>,
  getDesired: () => boolean,
): Promise<LikeResult> {
  let confirmed = await toggle();
  let toggles = 1;
  while (confirmed.isLiked !== getDesired() && toggles < MAX_TOGGLES) {
    confirmed = await toggle();
    toggles += 1;
  }
  return confirmed;
}
