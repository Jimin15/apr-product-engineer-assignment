"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { isPostGone } from "@/shared/api/ApiError";
import { errorMessage } from "@/shared/api/errorMessage";
import { useRequester } from "@/shared/hooks/useRequester";
import { canGoBackInApp, goBack, replaceRoute } from "@/shared/lib/goBack";
import { isOwner } from "@/shared/lib/requester";
import { LoadError, Loading } from "@/shared/ui/LoadError";
import { NotFoundDialog } from "@/shared/ui/NotFoundDialog";
import { useToast } from "@/shared/ui/Toast";
import { TopBar } from "@/shared/ui/TopBar";
import { PostForm } from "./PostForm";
import { postKeys, useCreatePost, usePost, useUpdatePost } from "./queries";

/** 작성 화면 (F-43) */
export function CreatePostScreen() {
  const router = useRouter();
  const toast = useToast();
  const create = useCreatePost();

  return (
    <PostForm
      mode="create"
      pending={create.isPending}
      onSubmit={(input, done) =>
        create.mutate(input, {
          onSettled: done,
          onSuccess: () => {
            router.push("/");
            toast("게시글이 등록되었습니다.");
          },
          onError: (error) => toast(errorMessage(error)),
        })
      }
    />
  );
}

/** 수정 화면 (F-44 · F-47): 작성자를 확인한 뒤에만 폼을 보여준다 */
export function EditPostScreen({ id }: { id: number }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const toast = useToast();
  const requester = useRequester();
  const [gone, setGone] = useState(!Number.isInteger(id));
  const markGone = useCallback(() => {
    setGone(true);
    queryClient.removeQueries({ queryKey: postKeys.detail(id) });
  }, [queryClient, id]);

  const query = usePost(id, !gone);
  const update = useUpdatePost(id);
  const post = query.data;
  const mine = post != null && requester != null && isOwner(post.author, requester);

  useEffect(() => {
    if (isPostGone(query.error)) markGone();
  }, [query.error, markGone]);

  // 남의 글: 폼을 보여주지 않고 상세로 보낸다 (FD-14)
  useEffect(() => {
    if (post && requester != null && !isOwner(post.author, requester)) {
      replaceRoute(router, `/posts/${id}`);
      toast("다른 사람의 글은 수정할 수 없습니다.");
    }
  }, [post, requester, router, id, toast]);

  if (gone) {
    return (
      <>
        <TopBar onBack={() => goBack(router)} />
        <NotFoundDialog onConfirm={() => replaceRoute(router, "/")} />
      </>
    );
  }

  if (!mine) {
    return (
      <>
        <TopBar onBack={() => goBack(router)} />
        {query.isError && !post ? (
          <LoadError onRetry={() => void query.refetch()} retrying={query.isFetching} />
        ) : (
          <Loading className="mt-40" />
        )}
      </>
    );
  }

  return (
    <PostForm
      mode="edit"
      initial={{ title: post.title, content: post.content }}
      pending={update.isPending}
      onSubmit={(input, done) =>
        update.mutate(input, {
          onSettled: done,
          onSuccess: () => {
            // 상세에서 들어왔으면 그 상세로 돌아가고(기록에 수정 화면이 남지 않음),
            // 주소로 직접 연 수정 화면이면 상세로 바꾼다.
            if (canGoBackInApp()) router.back();
            else replaceRoute(router, `/posts/${id}`);
            toast("게시글이 수정되었습니다.");
          },
          onError: (error) => {
            if (isPostGone(error)) markGone();
            else toast(errorMessage(error));
          },
        })
      }
    />
  );
}
