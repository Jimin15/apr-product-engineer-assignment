"use client";

import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { useSingleFlight } from "@/shared/hooks/useSingleFlight";
import { goBack } from "@/shared/lib/goBack";
import {
  TITLE_MAX,
  countCodePoints,
  isBlank,
  truncateCodePoints,
} from "@/shared/lib/text";
import { Dialog } from "@/shared/ui/Dialog";
import { TopBar } from "@/shared/ui/TopBar";
import type { PostInput } from "./api";

type Props = {
  mode: "create" | "edit";
  initial?: PostInput;
  pending: boolean;
  /** `done` 은 요청이 끝났을 때(성공 · 실패) 부른다 — 그 전에는 다시 제출하지 않는다 */
  onSubmit: (input: PostInput, done: () => void) => void;
};

const CONTENT_PLACEHOLDER =
  "게시글 내용을 작성해주세요.\n부적합한 내용은 삭제될 수 있으며, 비방/욕설 등의 내용으로 신고가 접수될 경우 서비스 이용이 제한될 수 있습니다.";

/** 작성 · 수정 공용 폼 (F-40 ~ F-46). 수정은 라벨 · 버튼 문구만 다르다 (FD-8). */
export function PostForm({ mode, initial, pending, onSubmit }: Props) {
  const router = useRouter();
  const [title, setTitle] = useState(initial?.title ?? "");
  const [content, setContent] = useState(initial?.content ?? "");
  const [confirmLeave, setConfirmLeave] = useState(false);
  const composing = useRef(false);
  const singleFlight = useSingleFlight();

  const count = countCodePoints(title);
  const valid = !isBlank(title) && !isBlank(content);
  const dirty =
    mode === "create"
      ? !isBlank(title) || !isBlank(content)
      : title !== initial?.title || content !== initial?.content;

  const handleBack = () => {
    if (dirty) setConfirmLeave(true);
    else goBack(router);
  };

  return (
    <>
      <TopBar onBack={handleBack} />
      <form
        className="flex min-h-[calc(100dvh-48px)] flex-col px-4 pb-[94px]"
        onSubmit={(e) => {
          e.preventDefault();
          if (valid && !pending)
            singleFlight((done) => onSubmit({ title, content }, done));
        }}
      >
        <h2 className="mt-[34px] text-14c font-bold text-ink">
          {mode === "create" ? "글작성" : "글수정"}
        </h2>

        <div
          className={`relative mt-[15px] h-[31px] border-b ${
            title ? "border-ink" : "border-underline"
          }`}
        >
          <input
            value={title}
            aria-label="제목"
            placeholder={`제목을 입력해 주세요. (최대 ${TITLE_MAX}자)`}
            onCompositionStart={() => {
              composing.current = true;
            }}
            onCompositionEnd={(e) => {
              // 한글 조합이 끝난 뒤에 자른다 (조합 중 자르면 글자가 깨진다)
              composing.current = false;
              setTitle(truncateCodePoints(e.currentTarget.value, TITLE_MAX));
            }}
            onChange={(e) => {
              const next = e.target.value;
              setTitle(composing.current ? next : truncateCodePoints(next, TITLE_MAX));
            }}
            className="h-[30px] w-full bg-transparent pr-[60px] pb-3 text-14 text-ink outline-none placeholder:text-muted"
          />
          <span
            className={`absolute top-[2px] right-0 text-12 ${
              count >= TITLE_MAX ? "text-brand" : "text-muted"
            }`}
          >
            {count} / {TITLE_MAX}
          </span>
        </div>

        <textarea
          value={content}
          aria-label="내용"
          placeholder={CONTENT_PLACEHOLDER}
          onChange={(e) => setContent(e.target.value)}
          className="mt-[30px] min-h-[200px] w-[326px] flex-1 resize-none bg-transparent text-14 text-ink outline-none placeholder:text-muted"
        />

        <div className="fixed bottom-0 left-1/2 z-10 w-[375px] -translate-x-1/2 bg-white px-4 pt-[6px] pb-8">
          <button
            type="submit"
            disabled={!valid || pending}
            className="h-14 w-full rounded-xl bg-ink text-16 font-bold text-white disabled:bg-disabled"
          >
            {mode === "create" ? "작성 완료" : "수정 완료"}
          </button>
        </div>
      </form>

      {confirmLeave && (
        <Dialog
          title="나가기"
          message={"작성 중인 내용이 저장되지 않습니다.\n정말 나가시겠습니까?"}
          onDismiss={() => setConfirmLeave(false)}
          buttons={[
            {
              label: "계속 작성",
              variant: "secondary",
              onClick: () => setConfirmLeave(false),
            },
            { label: "나가기", variant: "primary", onClick: () => goBack(router) },
          ]}
        />
      )}
    </>
  );
}
