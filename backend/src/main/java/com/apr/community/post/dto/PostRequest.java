package com.apr.community.post.dto;

import jakarta.validation.constraints.NotBlank;

import com.apr.community.common.validation.CodePointLength;

/** 게시글 작성 · 수정 본문 { title, content } (B-30 · B-31). 제목은 저장 전에 앞뒤 공백을 제거한다. */
public record PostRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @CodePointLength(max = 20, message = "제목은 20자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        String content) {

    public String normalizedTitle() {
        return title.strip();
    }
}
