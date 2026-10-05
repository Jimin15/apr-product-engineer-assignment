import { apiFetch } from "@/shared/api/apiFetch";
import type { Comment, Cursor, Page } from "@/shared/api/types";

export const FIRST_COMMENT_PAGE_SIZE = 2; // 최초 2개만 (F-31)
export const COMMENT_PAGE_SIZE = 20;

export type CommentPageParam = { size: number; cursor: Cursor | null };

export function fetchComments(
  postId: number,
  { size, cursor }: CommentPageParam,
  signal?: AbortSignal,
) {
  const query = new URLSearchParams({ size: String(size) });
  if (cursor) {
    query.set("cursorCreatedAt", cursor.createdAt);
    query.set("cursorId", String(cursor.id));
  }
  return apiFetch<Page<Comment>>(`/posts/${postId}/comments?${query}`, {
    signal,
  });
}

export function createComment(postId: number, content: string) {
  return apiFetch<Comment>(`/posts/${postId}/comments`, {
    method: "POST",
    body: { content },
  });
}

export function deleteComment(commentId: number) {
  return apiFetch<void>(`/comments/${commentId}`, { method: "DELETE" });
}
