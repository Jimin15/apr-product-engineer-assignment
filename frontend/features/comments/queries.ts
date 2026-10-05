"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from "@tanstack/react-query";
import { patchPostEverywhere, postKeys } from "@/features/posts/queries";
import type { Comment, Page } from "@/shared/api/types";
import {
  COMMENT_PAGE_SIZE,
  FIRST_COMMENT_PAGE_SIZE,
  createComment,
  deleteComment,
  fetchComments,
  type CommentPageParam,
} from "./api";

export const commentKeys = {
  list: (postId: number) => ["comments", postId] as const,
};

type CommentListData = InfiniteData<Page<Comment>, CommentPageParam>;

export function useComments(postId: number, enabled = true) {
  return useInfiniteQuery({
    queryKey: commentKeys.list(postId),
    queryFn: ({ pageParam, signal }) => fetchComments(postId, pageParam, signal),
    initialPageParam: {
      size: FIRST_COMMENT_PAGE_SIZE,
      cursor: null,
    } as CommentPageParam,
    getNextPageParam: (lastPage) =>
      lastPage.nextCursor
        ? { size: COMMENT_PAGE_SIZE, cursor: lastPage.nextCursor }
        : undefined,
    enabled,
  });
}

function invalidateAll(queryClient: QueryClient, postId: number) {
  void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
  void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
  void queryClient.invalidateQueries({ queryKey: postKeys.list });
}

export function useCreateComment(postId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (content: string) => createComment(postId, content),
    onSuccess: () => {
      patchPostEverywhere(queryClient, postId, (post) => ({
        ...post,
        commentCount: post.commentCount + 1,
      }));
      invalidateAll(queryClient, postId); // 새 댓글이 맨 위에 오도록 댓글 목록은 다시 받는다
    },
  });
}

/** 댓글 캐시에서 즉시 제거하고 total · commentCount 를 1 줄인다. */
export function removeCommentFromCache(
  queryClient: QueryClient,
  postId: number,
  commentId: number,
) {
  let removed = 0;
  queryClient.setQueryData<CommentListData>(commentKeys.list(postId), (old) => {
    if (!old) return old;
    const pages = old.pages.map((page) => {
      const items = page.items.filter((c) => c.id !== commentId);
      removed += page.items.length - items.length;
      return { ...page, items };
    });
    return {
      ...old,
      pages: pages.map((page) => ({ ...page, total: page.total - removed })),
    };
  });
  if (removed > 0) {
    patchPostEverywhere(queryClient, postId, (post) => ({
      ...post,
      commentCount: post.commentCount - removed,
    }));
  }
}

export function useDeleteComment(postId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onSuccess: (_, commentId) => {
      removeCommentFromCache(queryClient, postId, commentId);
      invalidateAll(queryClient, postId);
    },
  });
}
