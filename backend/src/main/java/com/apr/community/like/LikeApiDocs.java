package com.apr.community.like;

import static com.apr.community.common.error.ErrorCode.CONCURRENT_UPDATE;
import static com.apr.community.common.error.ErrorCode.POST_DELETED;
import static com.apr.community.common.error.ErrorCode.POST_NOT_FOUND;

import java.util.List;

import com.apr.community.common.docs.ApiErrorCodes;
import com.apr.community.common.docs.SchemaDoc;
import com.apr.community.like.dto.LikeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 좋아요 API 문서. */
@Tag(name = "좋아요")
public interface LikeApiDocs {

    /** 응답 스키마 설명 (DTO 대신 여기에 둔다) */
    List<SchemaDoc> SCHEMAS = List.of(
            SchemaDoc.type("LikeResponse", "좋아요 토글 결과 — 처리 후 상태"),
            SchemaDoc.field("LikeResponse", "isLiked", "요청자가 이제 좋아요한 상태인지", "true"),
            SchemaDoc.field("LikeResponse", "likeCount", "처리 후 좋아요 수", "1797"));

    @Operation(summary = "좋아요 토글",
            description = "요청자 기준으로 누르지 않았으면 좋아요, 눌렀으면 취소한다. 처리 후 상태를 돌려준다. "
                    + "같은 사용자의 동시 요청도 하나씩 순서대로 반영된다. 같은 글에 여러 요청이 몰려 재시도(최대 3회)로도 처리하지 못하면 409.")
    @ApiErrorCodes({POST_NOT_FOUND, POST_DELETED, CONCURRENT_UPDATE})
    LikeResponse toggle(@Parameter(description = "게시글 id", example = "1") Long postId, String userId);
}
