"use client";

import { useEffect, useState } from "react";
import { getRequester } from "@/shared/lib/requester";

/**
 * 요청자. 서버 렌더링에는 쿠키가 없으므로 마운트 뒤에만 값이 생긴다 (`null` = 아직 모름).
 * 본인 여부 표시는 값이 생긴 뒤에만 한다 (plan "서버에서 한 번 그려지는 점").
 */
export function useRequester(): string | null {
  const [requester, setRequester] = useState<string | null>(null);
  useEffect(() => {
    setRequester(getRequester());
  }, []);
  return requester;
}
