package com.apr.community.post;

import static com.apr.community.common.error.ErrorCode.INVALID_INPUT;
import static com.apr.community.common.error.ErrorCode.INVALID_QUERY;
import static com.apr.community.common.error.ErrorCode.NOT_OWNER;
import static com.apr.community.common.error.ErrorCode.POST_DELETED;
import static com.apr.community.common.error.ErrorCode.POST_NOT_FOUND;

import java.util.List;

import com.apr.community.common.docs.ApiErrorCodes;
import com.apr.community.common.docs.SchemaDoc;
import com.apr.community.common.paging.PageResponse;
import com.apr.community.post.dto.PostRequest;
import com.apr.community.post.dto.PostResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 게시글 API 문서. 요청자 헤더 · USER_ID_MISMATCH · 경로 변수 INVALID_INPUT 은 OpenApiConfig 가 자동으로 붙인다. */
@Tag(name = "게시글", description = "목록 · 상세 · 작성 · 수정 · 삭제")
public interface PostApiDocs {

    /** 요청 · 응답 스키마 설명 (DTO 대신 여기에 둔다) */
    List<SchemaDoc> SCHEMAS = List.of(
            SchemaDoc.type("PostResponse", "게시글. 목록 항목 · 상세 · 작성 · 수정 응답이 모두 이 형태"),
            SchemaDoc.field("PostResponse", "id", "게시글 id", "1"),
            SchemaDoc.field("PostResponse", "title", "제목 (최대 20자)", "부스터 프로 써보신 분?"),
            SchemaDoc.field("PostResponse", "content", "내용 (줄바꿈 포함 그대로)", "결론부터 말하면 저는 만족이에요."),
            SchemaDoc.field("PostResponse", "likeCount", "좋아요 수", "1796"),
            SchemaDoc.field("PostResponse", "commentCount", "삭제되지 않은 댓글 수", "2"),
            SchemaDoc.field("PostResponse", "isLiked", "요청자가 좋아요를 눌렀는지", "false"),
            SchemaDoc.field("PostResponse", "author", "작성자 id — 요청자와 같으면 본인 글", "apr_tester"),
            SchemaDoc.field("PostResponse", "createdAt", "작성 시각 (UTC, 초 단위)", "2026-08-30T13:55:54Z"),

            SchemaDoc.type("PostRequest", "게시글 작성 · 수정 본문"),
            SchemaDoc.field("PostRequest", "title", "제목. 앞뒤 공백 제거 후 1~20자 (유니코드 코드포인트 기준 — 이모지 🙏🏻 는 2)", "유쎄라 사용 꿀팁 공유"),
            SchemaDoc.field("PostRequest", "content", "내용. 공백 · 줄바꿈만이면 빈 값. 받은 그대로 저장, 길이 제한 없음", "일단 저는 각진턱이 고민입니다."));

    @Operation(summary = "게시글 목록",
            description = """
                    삭제된 글을 뺀 최신순 목록 (`createdAt` 내림차순, 같으면 `id` 내림차순). `isLiked`는 요청자 기준.
                    - `items`: 이번 묶음 (`size` 이하)
                    - `total`: 이번 응답 개수가 아니라 삭제를 뺀 전체 개수
                    - `nextCursor`: 다음 묶음 기준. 다음 요청에 `cursorCreatedAt` · `cursorId`로 보낸다. `null`이면 끝
                    """)
    @ApiErrorCodes(INVALID_QUERY)
    PageResponse<PostResponse> getPosts(
            @Parameter(description = "한 번에 받을 개수 (1~100, 생략 시 20)", example = "20") Integer size,
            @Parameter(description = "다음 묶음 기준 — 직전 응답 `nextCursor.createdAt`. `cursorId`와 함께 보낸다", example = "2026-08-22T06:45:54Z") String cursorCreatedAt,
            @Parameter(description = "다음 묶음 기준 — 직전 응답 `nextCursor.id`. `cursorCreatedAt`과 함께 보낸다", example = "21") Long cursorId,
            String userId);

    @Operation(summary = "게시글 작성",
            description = "제목 · 내용 필수. 제목은 앞뒤 공백을 제거해 저장하고 최대 20자(유니코드 코드포인트 기준). 내용은 받은 그대로 저장한다. "
                    + "작성자는 요청자. 201과 함께 만들어진 게시글(목록 항목과 같은 형태)을 돌려준다.")
    @ApiErrorCodes(INVALID_INPUT)
    PostResponse createPost(PostRequest request, String userId);

    @Operation(summary = "게시글 상세", description = "목록 항목과 같은 단일 객체. 삭제된 글은 `POST_DELETED`, 없는 글은 `POST_NOT_FOUND`.")
    @ApiErrorCodes({POST_NOT_FOUND, POST_DELETED})
    PostResponse getPost(@Parameter(description = "게시글 id", example = "1") Long postId, String userId);

    @Operation(summary = "게시글 수정",
            description = "본인 글만. 제약은 작성과 같다. 존재 · 삭제 여부를 먼저 확인하고 권한은 그다음이다 — 삭제된 남의 글은 403이 아니라 404.")
    @ApiErrorCodes({INVALID_INPUT, NOT_OWNER, POST_NOT_FOUND, POST_DELETED})
    PostResponse updatePost(@Parameter(description = "게시글 id", example = "1") Long postId, PostRequest request, String userId);

    @Operation(summary = "게시글 삭제",
            description = "본인 글만. 행을 지우지 않고 삭제 표시만 남긴다. 이후 목록 · 상세에서 빠지고 그 글의 댓글 · 좋아요 요청도 `POST_DELETED`.")
    @ApiErrorCodes({NOT_OWNER, POST_NOT_FOUND, POST_DELETED})
    void deletePost(@Parameter(description = "게시글 id", example = "1") Long postId, String userId);
}
