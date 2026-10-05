package com.apr.community.comment.dto;

import java.time.Instant;

import com.apr.community.comment.Comment;

/** 댓글 응답 (Integration spec §1.2 Comment) */
public record CommentResponse(Long id, Long postId, String content, String author, Instant createdAt) {

    public static CommentResponse of(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getContent(),
                comment.getAuthor(), comment.getCreatedAt());
    }
}
