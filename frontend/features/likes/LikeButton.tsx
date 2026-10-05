"use client";

import type { Post } from "@/shared/api/types";
import { formatCount } from "@/shared/lib/text";
import { HeartFilledIcon, HeartLineIcon } from "@/shared/ui/icons";
import { useLikeToggle } from "./useLikeToggle";

/** 상세 좋아요 (F-20 · F-21): 외곽선 `#979ea9` / 채움 `#ff8da1` + `좋아요 N` */
export function LikeButton({
  post,
  onGone,
}: {
  post: Post;
  onGone: () => void;
}) {
  const { isLiked, likeCount, toggle } = useLikeToggle(post, onGone);
  return (
    <button
      type="button"
      onClick={toggle}
      aria-pressed={isLiked}
      aria-label="좋아요"
      className="flex items-center gap-[2px]"
    >
      {isLiked ? (
        <HeartFilledIcon className="text-heart" />
      ) : (
        <HeartLineIcon className="text-muted" />
      )}
      <span className="text-13 text-muted">좋아요 {formatCount(likeCount)}</span>
    </button>
  );
}
