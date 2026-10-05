"use client";

import type { ReactNode } from "react";
import { ArrowBackIcon } from "./icons";

type Props = {
  title?: string;
  onBack?: () => void;
  right?: ReactNode;
  /** 목록 상단 바만 아래 1px 테두리가 있다 */
  border?: boolean;
};

/** 상단 바 48px (F-08 · F-16) */
export function TopBar({ title, onBack, right, border = false }: Props) {
  return (
    <header
      className={`sticky top-0 z-10 flex h-12 items-center bg-white ${
        border ? "border-b border-line" : ""
      }`}
    >
      {onBack && (
        <button
          type="button"
          onClick={onBack}
          aria-label="뒤로 가기"
          className="ml-4 flex size-[22px] items-center justify-center text-ink"
        >
          <ArrowBackIcon />
        </button>
      )}
      {title && <h1 className="ml-4 text-20 font-bold text-ink">{title}</h1>}
      {right && <div className="ml-auto mr-4 flex items-center">{right}</div>}
    </header>
  );
}
