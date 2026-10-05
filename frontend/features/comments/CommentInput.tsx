"use client";

import { useRef, useState } from "react";
import { isPostGone } from "@/shared/api/ApiError";
import { errorMessage } from "@/shared/api/errorMessage";
import { useSingleFlight } from "@/shared/hooks/useSingleFlight";
import { isBlank } from "@/shared/lib/text";
import { SendIcon } from "@/shared/ui/icons";
import { useToast } from "@/shared/ui/Toast";
import { useCreateComment } from "./queries";

const LINE_HEIGHT = 22;
const MIN_HEIGHT = 40; // 한 줄
const MAX_HEIGHT = MIN_HEIGHT + LINE_HEIGHT * 3; // 최대 4줄 (plan 세부값)

/** 하단 고정 댓글 입력 바 (F-34 · FD-1) */
export function CommentInput({
  postId,
  onGone,
}: {
  postId: number;
  onGone: () => void;
}) {
  const toast = useToast();
  const [value, setValue] = useState("");
  const textarea = useRef<HTMLTextAreaElement>(null);
  const create = useCreateComment(postId);
  const singleFlight = useSingleFlight();
  const canSend = !isBlank(value) && !create.isPending;

  const resize = (el: HTMLTextAreaElement) => {
    el.style.height = `${MIN_HEIGHT}px`;
    const next = Math.min(el.scrollHeight + 2, MAX_HEIGHT);
    el.style.height = `${next}px`;
    el.style.overflowY = next >= MAX_HEIGHT ? "auto" : "hidden"; // 4줄 넘을 때만 안에서 스크롤
  };

  const send = () => {
    if (!canSend) return;
    singleFlight((done) =>
      create.mutate(value, {
        onSuccess: () => {
          setValue("");
          const el = textarea.current;
          if (el) {
            el.style.height = `${MIN_HEIGHT}px`;
            el.blur(); // 키보드 닫기 → 새 댓글이 맨 위에 보이도록
          }
        },
        onError: (error) => {
          if (isPostGone(error)) onGone();
          else toast(errorMessage(error)); // 입력 내용은 유지
        },
        onSettled: done,
      }),
    );
  };

  return (
    <div className="fixed bottom-0 left-1/2 z-10 w-[375px] -translate-x-1/2 border-t border-line bg-white px-4 pt-[9px] pb-9">
      <div className="relative">
        <textarea
          ref={textarea}
          value={value}
          rows={1}
          enterKeyHint="enter"
          placeholder="댓글을 입력하세요."
          aria-label="댓글"
          onChange={(e) => {
            setValue(e.target.value);
            resize(e.target);
          }}
          className="block h-10 w-full resize-none overflow-y-hidden rounded-[20px] border border-line py-2 pl-5 pr-11 text-15 text-ink outline-none placeholder:text-muted"
        />
        {!isBlank(value) && (
          <button
            type="button"
            onClick={send}
            disabled={!canSend}
            aria-label="댓글 등록"
            className="absolute right-[6px] bottom-[6px] flex size-7 items-center justify-center rounded-full bg-brand text-white"
          >
            <SendIcon />
          </button>
        )}
      </div>
    </div>
  );
}
