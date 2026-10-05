import { apiFetch } from "@/shared/api/apiFetch";
import type { Cursor, Page, Post } from "@/shared/api/types";

export const POST_PAGE_SIZE = 20;

export type PostInput = { title: string; content: string };

export function fetchPosts(cursor: Cursor | null, signal?: AbortSignal) {
  const query = new URLSearchParams({ size: String(POST_PAGE_SIZE) });
  if (cursor) {
    query.set("cursorCreatedAt", cursor.createdAt);
    query.set("cursorId", String(cursor.id));
  }
  return apiFetch<Page<Post>>(`/posts?${query}`, { signal });
}

export function fetchPost(id: number, signal?: AbortSignal) {
  return apiFetch<Post>(`/posts/${id}`, { signal });
}

export function createPost(input: PostInput) {
  return apiFetch<Post>("/posts", { method: "POST", body: input });
}

export function updatePost(id: number, input: PostInput) {
  return apiFetch<Post>(`/posts/${id}`, { method: "PATCH", body: input });
}

export function deletePost(id: number) {
  return apiFetch<void>(`/posts/${id}`, { method: "DELETE" });
}
