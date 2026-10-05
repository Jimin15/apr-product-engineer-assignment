"use client";

import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { trackPath } from "@/shared/lib/goBack";

/** 앱 안 이동 순번을 기록한다 (뒤로 가기 판단용, F-08). */
export function HistoryTracker() {
  const pathname = usePathname();
  useEffect(() => {
    trackPath(pathname);
  }, [pathname]);
  return null;
}
