"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useRef, useState } from "react";
import { fetchPost } from "@/features/posts/api";
import { patchPostEverywhere, postKeys } from "@/features/posts/queries";
import { isPostGone } from "@/shared/api/ApiError";
import type { LikeResult, Post } from "@/shared/api/types";
import { useToast } from "@/shared/ui/Toast";
import { toggleLike } from "./api";
import { convergeLike, displayedCount } from "./likeState";

export const LIKE_FAILED_MESSAGE =
  "좋아요 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.";

/**
 * 게시글별 `desired`(마지막으로 누른 상태) · `confirmed`(서버 확인 상태) · `inFlight`
 * (plan "구현 규칙 · 좋아요").
 */
export function useLikeToggle(post: Post, onGone: () => void) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [confirmed, setConfirmed] = useState<LikeResult>({
    isLiked: post.isLiked,
    likeCount: post.likeCount,
  });
  const [desired, setDesired] = useState(post.isLiked);
  const desiredRef = useRef(desired);
  const inFlight = useRef(false);

  // 요청 중이 아닐 때만 서버 데이터(재조회 · 캐시 반영)를 따라간다.
  useEffect(() => {
    if (inFlight.current) return;
    setConfirmed({ isLiked: post.isLiked, likeCount: post.likeCount });
    setDesired(post.isLiked);
    desiredRef.current = post.isLiked;
  }, [post.isLiked, post.likeCount]);

  const applyConfirmed = useCallback(
    (result: LikeResult) => {
      setConfirmed(result);
      setDesired(result.isLiked);
      desiredRef.current = result.isLiked;
      patchPostEverywhere(queryClient, post.id, (p) => ({ ...p, ...result }));
    },
    [queryClient, post.id],
  );

  const run = useCallback(async () => {
    inFlight.current = true;
    // 진행 중인 재조회가 화면의 desired 를 덮지 않게 한다.
    await queryClient.cancelQueries({ queryKey: postKeys.detail(post.id) });
    try {
      const result = await convergeLike(
        () => toggleLike(post.id),
        () => desiredRef.current,
      );
      applyConfirmed(result);
    } catch (error) {
      if (isPostGone(error)) {
        onGone();
        return;
      }
      toast(LIKE_FAILED_MESSAGE);
      // 서버 상태로 되돌린다 (F-21)
      try {
        const fresh = await queryClient.fetchQuery({
          queryKey: postKeys.detail(post.id),
          queryFn: ({ signal }) => fetchPost(post.id, signal),
          staleTime: 0,
        });
        applyConfirmed({ isLiked: fresh.isLiked, likeCount: fresh.likeCount });
      } catch (refreshError) {
        if (isPostGone(refreshError)) onGone();
      }
    } finally {
      inFlight.current = false;
    }
  }, [queryClient, post.id, applyConfirmed, onGone, toast]);

  const toggle = useCallback(() => {
    const next = !desiredRef.current;
    desiredRef.current = next;
    setDesired(next); // 누르는 즉시 화면 반영
    if (!inFlight.current) void run();
  }, [run]);

  return {
    isLiked: desired,
    likeCount: displayedCount(confirmed, desired),
    toggle,
  };
}
