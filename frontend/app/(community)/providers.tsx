"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/shared/api/ApiError";
import { HistoryTracker } from "@/shared/hooks/HistoryTracker";
import { ToastProvider } from "@/shared/ui/Toast";

export function Providers({ children }: { children: React.ReactNode }) {
  // 렌더링마다 캐시가 새로 생기지 않도록 한 번만 만든다 (plan F11 "구조").
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            // 4xx 는 다시 보내도 결과가 같으므로 재시도하지 않는다 (plan 세부값).
            retry: (failureCount, error) =>
              failureCount < 1 &&
              !(error instanceof ApiError && error.status < 500),
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <HistoryTracker />
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}
