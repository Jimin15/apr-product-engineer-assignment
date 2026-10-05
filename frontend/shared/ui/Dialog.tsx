"use client";

export type DialogButton = {
  label: string;
  variant: "secondary" | "primary";
  onClick: () => void;
  disabled?: boolean;
};

type Props = {
  title?: string;
  message: string;
  /** 제목 없는 알림(F-53)은 문구를 굵게 */
  emphasizeMessage?: boolean;
  buttons: DialogButton[];
  /** 바깥을 눌렀을 때. 없으면 바깥 클릭으로 닫히지 않는다. */
  onDismiss?: () => void;
};

const BUTTON_STYLE = {
  secondary: "bg-line text-muted",
  primary: "bg-ink text-white",
} as const;

/** 삭제 확인 다이얼로그 틀 (F-51): 327 폭 · r20 · 검정 60% 오버레이 */
export function Dialog({
  title,
  message,
  emphasizeMessage = false,
  buttons,
  onDismiss,
}: Props) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={(e) => {
        if (e.target === e.currentTarget) onDismiss?.();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        className={`w-[327px] rounded-[20px] bg-white px-[26px] text-center ${
          title ? "pt-[50px] pb-10" : "pt-10 pb-[26px]"
        }`}
      >
        {title && <h2 className="text-20 font-bold text-ink">{title}</h2>}
        <p
          className={`whitespace-pre-line ${title ? "mt-[14px]" : ""} ${
            emphasizeMessage
              ? "text-18 font-bold text-ink"
              : "text-16 text-muted"
          }`}
        >
          {message}
        </p>
        <div className="mt-7 flex gap-[15px]">
          {buttons.map((button) => (
            <button
              key={button.label}
              type="button"
              onClick={button.onClick}
              disabled={button.disabled}
              className={`h-14 flex-1 rounded-xl text-16 font-bold ${BUTTON_STYLE[button.variant]}`}
            >
              {button.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
