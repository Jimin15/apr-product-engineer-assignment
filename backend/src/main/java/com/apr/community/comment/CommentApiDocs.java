package com.apr.community.comment;

import static com.apr.community.common.error.ErrorCode.COMMENT_DELETED;
import static com.apr.community.common.error.ErrorCode.COMMENT_NOT_FOUND;
import static com.apr.community.common.error.ErrorCode.INVALID_INPUT;
import static com.apr.community.common.error.ErrorCode.INVALID_QUERY;
import static com.apr.community.common.error.ErrorCode.NOT_OWNER;
import static com.apr.community.common.error.ErrorCode.POST_DELETED;
import static com.apr.community.common.error.ErrorCode.POST_NOT_FOUND;

import java.util.List;

import com.apr.community.comment.dto.CommentRequest;
import com.apr.community.comment.dto.CommentResponse;
import com.apr.community.common.docs.ApiErrorCodes;
import com.apr.community.common.docs.SchemaDoc;
import com.apr.community.common.paging.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 댓글 API 문서. 댓글은 항상 부모 게시글 상태를 먼저 확인한다 — 게시글이 삭제됐으면 POST_DELETED. */
@Tag(name = "댓글", description = "목록 · 작성 · 삭제 (댓글 수정은 없다)")
public interface CommentApiDocs {

    /** 요청 · 응답 스키마 설명 (DTO 대신 여기에 둔다) */
    List<SchemaDoc> SCHEMAS = List.of(
            SchemaDoc.type("CommentResponse", "댓글"),
            SchemaDoc.field("CommentResponse", "id", "댓글 id", "2"),
            SchemaDoc.field("CommentResponse", "postId", "게시글 id", "1"),
            SchemaDoc.field("CommentResponse", "content", "내용 (줄바꿈 포함 그대로)", "저도 써봤는데 좋아요!"),
            SchemaDoc.field("CommentResponse", "author", "작성자 id — 요청자와 같으면 본인 댓글", "cos_holic"),
            SchemaDoc.field("CommentResponse", "createdAt", "작성 시각 (UTC, 초 단위)", "2026-08-30T14:02:33Z"),

            SchemaDoc.type("CommentRequest", "댓글 작성 본문"),
            SchemaDoc.field("CommentRequest", "content", "내용. 공백 · 줄바꿈만이면 빈 값. 받은 그대로 저장", "저도 써봤는데 좋아요!"));

    @Operation(summary = "댓글 목록",
            description = """
                    삭제된 댓글을 뺀 최신순 목록 (`createdAt` 내림차순, 같으면 `id` 내림차순). 요청자는 필요 없다.
                    화면은 처음에 `size=2`로 2개만 받고, 더보기 이후 `nextCursor`로 이어 받는다.
                    - `items`: 이번 묶음 (`size` 이하)
                    - `total`: 이번 응답 개수가 아니라 삭제를 뺀 전체 댓글 수 (게시글의 `commentCount`와 같다)
                    - `nextCursor`: 다음 묶음 기준. 다음 요청에 `cursorCreatedAt` · `cursorId`로 보낸다. `null`이면 끝
                    """)
    @ApiErrorCodes({INVALID_QUERY, POST_NOT_FOUND, POST_DELETED})
    PageResponse<CommentResponse> getComments(
            @Parameter(description = "게시글 id", example = "2") Long postId,
            @Parameter(description = "한 번에 받을 개수 (1~100, 생략 시 20)", example = "2") Integer size,
            @Parameter(description = "다음 묶음 기준 — 직전 응답 `nextCursor.createdAt`. `cursorId`와 함께 보낸다", example = "2026-08-30T14:06:51Z") String cursorCreatedAt,
            @Parameter(description = "다음 묶음 기준 — 직전 응답 `nextCursor.id`. `cursorCreatedAt`과 함께 보낸다", example = "1469") Long cursorId);

    @Operation(summary = "댓글 작성",
            description = "내용 필수(공백 · 줄바꿈만이면 빈 값). 내용은 받은 그대로 저장한다. 작성자는 요청자. 201과 함께 만들어진 댓글을 돌려준다.")
    @ApiErrorCodes({INVALID_INPUT, POST_NOT_FOUND, POST_DELETED})
    CommentResponse createComment(@Parameter(description = "게시글 id", example = "1") Long postId, CommentRequest request, String userId);

    @Operation(summary = "댓글 삭제",
            description = "본인 댓글만. 행을 지우지 않고 삭제 표시만 남긴다. 확인 순서: 댓글 존재 → 부모 게시글 상태 → 댓글 삭제 여부 → 권한.")
    @ApiErrorCodes({NOT_OWNER, COMMENT_NOT_FOUND, COMMENT_DELETED, POST_DELETED})
    void deleteComment(@Parameter(description = "댓글 id", example = "2") Long commentId, String userId);
}
