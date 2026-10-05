import { apiFetch } from "@/shared/api/apiFetch";
import type { LikeResult } from "@/shared/api/types";

export function toggleLike(postId: number) {
  return apiFetch<LikeResult>(`/posts/${postId}/likes/toggle`, {
    method: "POST",
  });
}
