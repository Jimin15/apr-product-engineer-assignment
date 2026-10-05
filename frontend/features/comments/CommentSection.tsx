"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { isCommentGone, isPostGone } from "@/shared/api/ApiError";
import { errorMessage } from "@/shared/api/errorMessage";
import { useInView } from "@/shared/hooks/useInView";
import { useSingleFlight } from "@/shared/hooks/useSingleFlight";
import { isOwner } from "@/shared/lib/requester";
import { formatCount } from "@/shared/lib/text";
import { Dialog } from "@/shared/ui/Dialog";
import { ChevronDownIcon } from "@/shared/ui/icons";
import { LoadError, Loading } from "@/shared/ui/LoadError";
import { useToast } from "@/shared/ui/Toast";
import { CommentItem } from "./CommentItem";
import {
  commentKeys,
  removeCommentFromCache,
  useComments,
  useDeleteComment,
} from "./queries";

type Props = {
  postId: number;
  /** 댓글 목록을 받기 전까지 보여줄 수 (상세의 commentCount) */
  fallbackCount: number;
  requester: string | null;
  onGone: () => void;
};

/** 댓글 영역 (F-30 ~ F-36): 최초 2개 → 더보기 → 무한 스크롤 */
export function CommentSection({ postId, fallbackCount, requester, onGone }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const query = useComments(postId);
  const [expanded, setExpanded] = useState(false);
  const [deleting, setDeleting] = useState<number | null>(null);
  const remove = useDeleteComment(postId);
  const singleFlight = useSingleFlight();

  useEffect(() => {
    if (isPostGone(query.error)) onGone();
  }, [query.error, onGone]);

  const comments = query.data?.pages.flatMap((page) => page.items) ?? [];
  const total = query.data?.pages[0]?.total ?? fallbackCount;

  const canLoadMore =
    expanded &&
    query.hasNextPage &&
    !query.isFetchingNextPage &&
    !query.isFetchNextPageError;
  const loadMore = useCallback(() => {
    if (canLoadMore) void query.fetchNextPage();
  }, [canLoadMore, query]);
  const sentinel = useInView(loadMore, canLoadMore);

  const showMoreButton =
    !expanded && query.data != null && total >= 3 && query.hasNextPage;
  const firstLoadFailed = query.isError && !query.data;

  const handleDelete = () => {
    if (deleting == null) return;
    const commentId = deleting;
    singleFlight((done) =>
      remove.mutate(commentId, {
        onSuccess: () => {
          setDeleting(null);
          toast("댓글이 삭제되었습니다.");
        },
        onError: (error) => {
          setDeleting(null);
          if (isPostGone(error)) {
            onGone();
          } else if (isCommentGone(error)) {
            // 이미 사라진 댓글: 목록에서 지우고 서버와 다시 맞춘다
            removeCommentFromCache(queryClient, postId, commentId);
            void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
          } else {
            toast(errorMessage(error));
          }
        },
        onSettled: done,
      }),
    );
  };

  return (
    <section>
      <div className="flex h-12 items-end gap-1 border-b border-line-2 px-4 pb-2 text-16 font-bold text-ink">
        <span>댓글</span>
        <span>({formatCount(total)})</span>
      </div>

      {query.isPending && <Loading />}
      {firstLoadFailed && (
        <LoadError onRetry={() => void query.refetch()} retrying={query.isFetching} />
      )}

      {query.data && total === 0 && (
        <p className="py-10 text-center text-14 text-muted">아직 댓글이 없습니다.</p>
      )}

      {comments.length > 0 && (
        <ul className="flex flex-col gap-4 px-4 pt-4 pb-4">
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              mine={requester != null && isOwner(comment.author, requester)}
              onDelete={() => setDeleting(comment.id)}
            />
          ))}
        </ul>
      )}

      {showMoreButton && (
        <div className="px-4 pt-2 pb-5">
          <button
            type="button"
            onClick={() => {
              setExpanded(true);
              void query.fetchNextPage();
            }}
            className="flex h-[42px] w-full items-center justify-center gap-1 rounded-xl border border-line-3 bg-white text-14m font-medium text-muted"
          >
            댓글 더보기
            <ChevronDownIcon />
          </button>
        </div>
      )}

      {query.isFetchingNextPage && <Loading />}
      {query.isFetchNextPageError && (
        <LoadError
          onRetry={() => void query.fetchNextPage()}
          retrying={query.isFetchingNextPage}
        />
      )}
      <div ref={sentinel} aria-hidden />

      {deleting != null && (
        <Dialog
          title="삭제하기"
          message={"삭제한 댓글은 복구할 수 없습니다.\n정말 삭제 하시겠습니까?"}
          onDismiss={() => setDeleting(null)}
          buttons={[
            { label: "취소", variant: "secondary", onClick: () => setDeleting(null) },
            {
              label: "예",
              variant: "primary",
              onClick: handleDelete,
              disabled: remove.isPending,
            },
          ]}
        />
      )}
    </section>
  );
}
