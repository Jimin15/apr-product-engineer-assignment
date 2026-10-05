"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";

const TOAST_DURATION_MS = 2000; // plan 세부값

type ShowToast = (message: string) => void;

const ToastContext = createContext<ShowToast>(() => {});

/** 화면 이동 뒤에도 보여야 하는 유일한 전역 상태 (plan F4). */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [message, setMessage] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const show = useCallback<ShowToast>((next) => {
    setMessage(next); // 새 토스트가 오면 이전 것을 교체
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setMessage(null), TOAST_DURATION_MS);
  }, []);

  useEffect(() => () => clearTimeout(timer.current), []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      {message && (
        <div
          role="status"
          className="pointer-events-none fixed bottom-[81px] left-1/2 z-[60] flex w-[375px] -translate-x-1/2 justify-center px-4"
        >
          <div className="rounded-[6px] bg-black/70 px-5 py-[11px] text-14 text-white">
            {message}
          </div>
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): ShowToast {
  return useContext(ToastContext);
}
