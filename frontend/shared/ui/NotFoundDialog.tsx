"use client";

import { Dialog } from "./Dialog";

/** 삭제되었거나 없는 게시글 알림 (F-53 · FD-5). 바깥을 눌러도 닫히지 않는다. */
export function NotFoundDialog({ onConfirm }: { onConfirm: () => void }) {
  return (
    <Dialog
      message={"삭제되었거나\n존재하지 않는 게시글입니다."}
      emphasizeMessage
      buttons={[{ label: "목록으로", variant: "primary", onClick: onConfirm }]}
    />
  );
}
