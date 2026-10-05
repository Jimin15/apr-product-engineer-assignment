package com.apr.community.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 좋아요 토글 응답 { isLiked, likeCount } — 처리 후 상태 (B-40) */
public record LikeResponse(@JsonProperty("isLiked") boolean isLiked, int likeCount) {
}
