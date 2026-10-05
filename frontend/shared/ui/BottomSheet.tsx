"use client";

import type { ReactNode } from "react";

export type SheetItem = { icon: ReactNode; label: string; onClick: () => void };

/** 본인 글 메뉴 바텀시트 (F-50). 위 모서리 r20, 검정 60% 오버레이. */
export function BottomSheet({
  items,
  onClose,
}: {
  items: SheetItem[];
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 bg-black/60" onClick={onClose}>
      <div
        role="menu"
        className="absolute bottom-0 left-1/2 w-[375px] -translate-x-1/2 rounded-t-[20px] bg-white pt-[26px]"
        onClick={(e) => e.stopPropagation()}
      >
        {items.map((item, index) => (
          <div key={item.label}>
            {index > 0 && <div className="mx-6 mt-5 mb-[19px] h-px bg-line" />}
            <button
              type="button"
              role="menuitem"
              onClick={item.onClick}
              className="flex h-[22px] w-full items-center justify-center gap-[6px] text-14c font-semibold text-ink"
            >
              {item.icon}
              {item.label}
            </button>
          </div>
        ))}
        <div className="mt-5 px-4 pt-[6px] pb-8">
          <button
            type="button"
            onClick={onClose}
            className="h-14 w-full rounded-xl bg-ink text-16 font-bold text-white"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  );
}
