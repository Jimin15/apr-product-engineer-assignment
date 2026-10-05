"use client";

import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from "@tanstack/react-query";
import type { Cursor, Page, Post } from "@/shared/api/types";
import {
  createPost,
  deletePost,
  fetchPost,
  fetchPosts,
  updatePost,
  type PostInput,
} from "./api";

// 쿼리 키 (plan "쿼리 키와 캐시 갱신")
export const postKeys = {
  list: ["posts", "list"] as const,
  detail: (id: number) => ["posts", "detail", id] as const,
};

type PostListData = InfiniteData<Page<Post>, Cursor | null>;

export function usePostList() {
  return useInfiniteQuery({
    queryKey: postKeys.list,
    queryFn: ({ pageParam, signal }) => fetchPosts(pageParam, signal),
    initialPageParam: null as Cursor | null,
    getNextPageParam: (lastPage) => lastPage.nextCursor, // null 이면 끝 (F-15)
  });
}

export function usePost(id: number, enabled = true) {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: ({ signal }) => fetchPost(id, signal),
    enabled,
  });
}

// ---- 캐시 갱신 (즉시 반영 → 무효화, F-09) ----

/** 목록 캐시의 한 항목을 고친다 (모든 페이지에서 찾는다). */
export function patchPostInList(
  queryClient: QueryClient,
  id: number,
  patch: (post: Post) => Post,
) {
  queryClient.setQueryData<PostListData>(postKeys.list, (old) =>
    old
      ? {
          ...old,
          pages: old.pages.map((page) => ({
            ...page,
            items: page.items.map((p) => (p.id === id ? patch(p) : p)),
          })),
        }
      : old,
  );
}

/** 상세 · 목록 캐시 양쪽에 같은 변경을 적용한다. */
export function patchPostEverywhere(
  queryClient: QueryClient,
  id: number,
  patch: (post: Post) => Post,
) {
  queryClient.setQueryData<Post>(postKeys.detail(id), (old) =>
    old ? patch(old) : old,
  );
  patchPostInList(queryClient, id, patch);
}

function prependPostToList(queryClient: QueryClient, post: Post) {
  // 새 글이 가장 최신이므로 첫 페이지 맨 앞이 정확한 위치다. 페이지 끝은 자르지 않는다 (plan).
  queryClient.setQueryData<PostListData>(postKeys.list, (old) =>
    old
      ? {
          ...old,
          pages: old.pages.map((page, index) => ({
            ...page,
            total: page.total + 1,
            items: index === 0 ? [post, ...page.items] : page.items,
          })),
        }
      : old,
  );
}

function removePostFromList(queryClient: QueryClient, id: number) {
  queryClient.setQueryData<PostListData>(postKeys.list, (old) =>
    old
      ? {
          ...old,
          pages: old.pages.map((page) => {
            const items = page.items.filter((p) => p.id !== id);
            return {
              ...page,
              items,
              total: page.total - (page.items.length - items.length),
            };
          }),
        }
      : old,
  );
}

// ---- 변경 ----

export function useCreatePost() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PostInput) => createPost(input),
    onSuccess: (post) => {
      prependPostToList(queryClient, post);
      queryClient.setQueryData(postKeys.detail(post.id), post);
      void queryClient.invalidateQueries({ queryKey: postKeys.list });
    },
  });
}

export function useUpdatePost(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PostInput) => updatePost(id, input),
    onSuccess: (post) => {
      queryClient.setQueryData(postKeys.detail(id), post);
      patchPostInList(queryClient, id, () => post);
      void queryClient.invalidateQueries({ queryKey: postKeys.list });
    },
  });
}

export function useDeletePost(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => deletePost(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: postKeys.detail(id) });
      removePostFromList(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: postKeys.list });
    },
  });
}
