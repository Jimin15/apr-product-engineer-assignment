"use client";

import type { Comment } from "@/shared/api/types";
import { commentTime } from "@/shared/lib/time";
import { Avatar } from "@/shared/ui/Avatar";

/** 댓글 항목 (F-33): 아바타 30 · 내용 폭 264 · 본인이면 내용 아래 왼쪽 `삭제` */
export function CommentItem({
  comment,
  mine,
  onDelete,
}: {
  comment: Comment;
  mine: boolean;
  onDelete: () => void;
}) {
  return (
    <li className="flex gap-[10px]">
      <Avatar author={comment.author} size={30} />
      <div className="w-[264px]">
        <div className="flex h-[22px] items-center gap-[6px]">
          <span className="text-14c font-bold text-ink">{comment.author}</span>
          <span className="text-12 text-muted">{commentTime(comment.createdAt)}</span>
        </div>
        <p className="mt-[6px] whitespace-pre-wrap break-words text-14 text-ink">
          {comment.content}
        </p>
        {mine && (
          <button
            type="button"
            onClick={onDelete}
            className="mt-3 block text-12 text-muted"
          >
            삭제
          </button>
        )}
      </div>
    </li>
  );
}
