"use client";

import Link from "next/link";
import { useCallback } from "react";
import { useInView } from "@/shared/hooks/useInView";
import { PencilIcon } from "@/shared/ui/icons";
import { LoadError, Loading } from "@/shared/ui/LoadError";
import { TopBar } from "@/shared/ui/TopBar";
import { PostListItem } from "./PostListItem";
import { usePostList } from "./queries";

/** 목록 화면 (F-10 ~ F-16) */
export function PostListScreen() {
  const query = usePostList();
  const posts = query.data?.pages.flatMap((page) => page.items) ?? [];

  const canLoadMore =
    query.hasNextPage &&
    !query.isFetchingNextPage &&
    !query.isFetchNextPageError;
  const loadMore = useCallback(() => {
    if (canLoadMore) void query.fetchNextPage();
  }, [canLoadMore, query]);
  const sentinel = useInView(loadMore, canLoadMore);

  const firstLoadFailed = query.isError && !query.data;

  return (
    <>
      <TopBar title="메디큐브톡" border />
      {/* 피그마: 상단 바 아래 8px 여백 뒤 첫 항목 (y=56) */}
      <main className="pt-2 pb-[120px]">
        <ul>
          {posts.map((post) => (
            <PostListItem key={post.id} post={post} />
          ))}
        </ul>
        {(query.isPending || query.isFetchingNextPage) && <Loading />}
        {firstLoadFailed && (
          <LoadError
            onRetry={() => void query.refetch()}
            retrying={query.isFetching}
          />
        )}
        {query.isFetchNextPageError && (
          <LoadError
            onRetry={() => void query.fetchNextPage()}
            retrying={query.isFetchingNextPage}
          />
        )}
        <div ref={sentinel} aria-hidden />
      </main>

      {/* 작성 버튼 (F-13): 375 틀 기준 오른쪽 15 · 아래 49 */}
      <div className="pointer-events-none fixed bottom-[49px] left-1/2 z-10 w-[375px] -translate-x-1/2">
        <Link
          href="/posts/new"
          aria-label="글쓰기"
          className="pointer-events-auto absolute right-[15px] bottom-0 block size-[50px] rounded-full bg-brand text-white"
        >
          <PencilIcon />
        </Link>
      </div>
    </>
  );
}
