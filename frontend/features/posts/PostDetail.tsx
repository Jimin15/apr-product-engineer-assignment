"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { CommentInput } from "@/features/comments/CommentInput";
import { CommentSection } from "@/features/comments/CommentSection";
import { commentKeys } from "@/features/comments/queries";
import { LikeButton } from "@/features/likes/LikeButton";
import { isPostGone } from "@/shared/api/ApiError";
import { errorMessage } from "@/shared/api/errorMessage";
import type { Post } from "@/shared/api/types";
import { useRequester } from "@/shared/hooks/useRequester";
import { useSingleFlight } from "@/shared/hooks/useSingleFlight";
import { goBack, replaceRoute } from "@/shared/lib/goBack";
import { isOwner } from "@/shared/lib/requester";
import { formatDate } from "@/shared/lib/time";
import { Avatar } from "@/shared/ui/Avatar";
import { BottomSheet } from "@/shared/ui/BottomSheet";
import { Dialog } from "@/shared/ui/Dialog";
import { MoreVerticalIcon, TrashIcon, WriteIcon } from "@/shared/ui/icons";
import { LoadError, Loading } from "@/shared/ui/LoadError";
import { NotFoundDialog } from "@/shared/ui/NotFoundDialog";
import { useToast } from "@/shared/ui/Toast";
import { TopBar } from "@/shared/ui/TopBar";
import { postKeys, useDeletePost, usePost } from "./queries";

/** 상세 화면 (F-20 ~ F-25) */
export function PostDetailScreen({ id }: { id: number }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const toast = useToast();
  const requester = useRequester();

  // 삭제되었거나 없는 글: 뒤 내용을 지우고 상단 바만 남긴다 (F-25)
  const [gone, setGone] = useState(!Number.isInteger(id));
  const markGone = useCallback(() => {
    setGone(true);
    // 댓글 캐시에 남은 POST_DELETED 오류로 다음 진입을 판단하지 않도록 함께 지운다
    queryClient.removeQueries({ queryKey: postKeys.detail(id) });
    queryClient.removeQueries({ queryKey: commentKeys.list(id) });
  }, [queryClient, id]);

  const query = usePost(id, !gone);
  useEffect(() => {
    if (isPostGone(query.error)) markGone();
  }, [query.error, markGone]);

  const [sheetOpen, setSheetOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const remove = useDeletePost(id);
  const singleFlight = useSingleFlight();

  const back = () => goBack(router);

  if (gone) {
    return (
      <>
        <TopBar onBack={back} />
        <NotFoundDialog onConfirm={() => replaceRoute(router, "/")} />
      </>
    );
  }

  const post = query.data;
  const mine = post != null && requester != null && isOwner(post.author, requester);

  const handleDelete = () =>
    singleFlight((done) =>
      remove.mutate(undefined, {
        onSuccess: () => {
          replaceRoute(router, "/");
          toast("게시글이 삭제되었습니다.");
        },
        onError: (error) => {
          setConfirmDelete(false);
          if (isPostGone(error)) markGone();
          else toast(errorMessage(error));
        },
        onSettled: done,
      }),
    );

  return (
    <>
      <TopBar
        onBack={back}
        right={
          mine && (
            <button
              type="button"
              onClick={() => setSheetOpen(true)}
              aria-label="메뉴"
              className="flex size-[22px] items-center justify-center text-ink"
            >
              <MoreVerticalIcon />
            </button>
          )
        }
      />

      {query.isPending && <Loading className="mt-40" />}
      {query.isError && !post && (
        <LoadError onRetry={() => void query.refetch()} retrying={query.isFetching} />
      )}

      {post && (
        <main className="pb-[86px]">
          <PostBody post={post} onGone={markGone} />
          <div className="h-2 bg-band" />
          <CommentSection
            postId={id}
            fallbackCount={post.commentCount}
            requester={requester}
            onGone={markGone}
          />
        </main>
      )}
      {post && <CommentInput postId={id} onGone={markGone} />}

      {sheetOpen && (
        <BottomSheet
          onClose={() => setSheetOpen(false)}
          items={[
            {
              icon: <WriteIcon />,
              label: "수정하기",
              onClick: () => router.push(`/posts/${id}/edit`),
            },
            {
              icon: <TrashIcon />,
              label: "삭제하기",
              onClick: () => {
                setSheetOpen(false);
                setConfirmDelete(true);
              },
            },
          ]}
        />
      )}

      {confirmDelete && (
        <Dialog
          title="삭제하기"
          message={"삭제한 글은 복구할 수 없습니다.\n정말 삭제 하시겠습니까?"}
          onDismiss={() => setConfirmDelete(false)}
          buttons={[
            {
              label: "취소",
              variant: "secondary",
              onClick: () => setConfirmDelete(false),
            },
            {
              label: "예",
              variant: "primary",
              onClick: handleDelete,
              disabled: remove.isPending,
            },
          ]}
        />
      )}
    </>
  );
}

/** 본문 (F-20): 제목 · 작성자 · 날짜 · 내용 · 좋아요 */
function PostBody({ post, onGone }: { post: Post; onGone: () => void }) {
  return (
    // 피그마: 상단 바 아래 8px + 본문 위 여백 24px (제목 y=80)
    <section className="px-4 pt-8 pb-3">
      <h2 className="text-18 font-bold text-ink">{post.title}</h2>
      <div className="mt-[18px] flex h-10 items-center gap-[10px]">
        <Avatar author={post.author} size={36} />
        <div>
          <p className="text-15 font-bold text-ink">{post.author}</p>
          <p className="mt-[2px] text-12 text-muted">{formatDate(post.createdAt)}</p>
        </div>
      </div>
      <p className="mt-6 w-[340px] whitespace-pre-wrap break-words text-14 text-ink">
        {post.content}
      </p>
      <div className="mt-8">
        <LikeButton post={post} onGone={onGone} />
      </div>
    </section>
  );
}
