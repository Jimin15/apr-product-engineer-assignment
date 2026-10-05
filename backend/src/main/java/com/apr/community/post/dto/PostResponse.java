package com.apr.community.post.dto;

import java.time.Instant;

import com.apr.community.post.Post;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 게시글 응답 (Integration spec §1.2 Post). 목록 항목과 상세가 같은 형태다. */
public record PostResponse(Long id,
                           String title,
                           String content,
                           int likeCount,
                           long commentCount,
                           @JsonProperty("isLiked") boolean isLiked,
                           String author,
                           Instant createdAt) {

    public static PostResponse of(Post post, int likeCount, long commentCount, boolean isLiked) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(),
                likeCount, commentCount, isLiked, post.getAuthor(), post.getCreatedAt());
    }
}
