"use client";

import Link from "next/link";
import type { Post } from "@/shared/api/types";
import { formatCount } from "@/shared/lib/text";
import { relativeTime } from "@/shared/lib/time";
import { Avatar } from "@/shared/ui/Avatar";
import { CommentIcon, HeartSmallIcon } from "@/shared/ui/icons";

/** 목록 항목 (F-11): 높이 118, 텍스트 폭 264, 하단 구분선 343 */
export function PostListItem({ post }: { post: Post }) {
  return (
    <li className="relative h-[118px]">
      <Link href={`/posts/${post.id}`} className="block h-full px-4 pt-3">
        <div className="flex h-[18px] items-center gap-[6px]">
          <Avatar author={post.author} size={18} />
          <span className="text-12 text-author">{post.author}</span>
        </div>
        <div className="mt-1 w-[264px]">
          <p className="truncate text-16 font-medium text-ink">{post.title}</p>
          <p className="mt-1 truncate text-14 text-muted">{post.content}</p>
        </div>
        <div className="mt-[9px] flex h-4 items-center text-12 text-muted">
          <HeartSmallIcon className="text-disabled" />
          <span className="ml-[2px]">{formatCount(post.likeCount)}</span>
          <CommentIcon className="ml-3 text-disabled" />
          <span className="ml-[2px]">{formatCount(post.commentCount)}</span>
          <span className="ml-auto">{relativeTime(post.createdAt)}</span>
        </div>
      </Link>
      <div className="absolute inset-x-4 bottom-0 h-px bg-line" />
    </li>
  );
}
