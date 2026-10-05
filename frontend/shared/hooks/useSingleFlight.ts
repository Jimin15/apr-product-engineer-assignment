"use client";

import { useCallback, useRef } from "react";

/**
 * 요청이 끝나기 전에는 같은 동작을 다시 실행하지 않는다 (중복 제출 방지).
 * 버튼 `disabled` 는 다시 그려진 뒤에야 적용되므로, 그 전에 들어온 연타도 막기 위해 ref 로 잠근다.
 *
 * 사용: `run((done) => mutation.mutate(input, { onSettled: done }))`
 */
export function useSingleFlight() {
  const busy = useRef(false);
  return useCallback((start: (done: () => void) => void) => {
    if (busy.current) return;
    busy.current = true;
    start(() => {
      busy.current = false;
    });
  }, []);
}
