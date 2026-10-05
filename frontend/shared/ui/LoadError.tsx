"use client";

/** 불러오기 실패 안내 (F-63 · FD-15). 버튼은 `댓글 더보기` 디자인 재사용. */
export function LoadError({
  onRetry,
  retrying = false,
}: {
  onRetry: () => void;
  retrying?: boolean;
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-4 py-10">
      <p className="text-14 text-muted">불러오지 못했습니다.</p>
      <button
        type="button"
        onClick={onRetry}
        disabled={retrying}
        className="flex h-[42px] w-full items-center justify-center rounded-xl border border-line-3 bg-white text-14m font-medium text-muted"
      >
        다시 시도
      </button>
    </div>
  );
}

/** 불러오는 중 표시 (plan 세부값) */
export function Loading({ className = "" }: { className?: string }) {
  return (
    <p className={`py-4 text-center text-12 text-muted ${className}`}>
      불러오는 중…
    </p>
  );
}
