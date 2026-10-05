package com.apr.community.comment.dto;

import jakarta.validation.constraints.NotBlank;

/** 댓글 작성 본문 { content }. 내용은 받은 그대로 저장한다 (B-30). */
public record CommentRequest(@NotBlank(message = "댓글 내용을 입력해 주세요.") String content) {
}
