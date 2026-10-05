"use client";

import { useEffect, useRef } from "react";

/**
 * sentinel 요소가 화면에 들어오면 `onVisible` 을 부른다 (무한 스크롤).
 * `enabled` 가 바뀔 때마다 observer 를 다시 만들어, 다음 묶음을 받은 뒤에도
 * sentinel 이 여전히 보이면 이어서 불러온다.
 */
export function useInView(onVisible: () => void, enabled: boolean) {
  const ref = useRef<HTMLDivElement>(null);
  const callback = useRef(onVisible);
  callback.current = onVisible;

  useEffect(() => {
    const el = ref.current;
    if (!enabled || !el) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) callback.current();
      },
      { rootMargin: "200px 0px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [enabled]);

  return ref;
}
