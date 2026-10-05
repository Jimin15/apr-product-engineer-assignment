package com.apr.community.post;

/**
 * 게시글별 댓글 수 조회용 프로젝션.
 * 목록 · 상세 응답의 commentCount 를 게시글 묶음 단위로 한 번에 세어 N+1 을 피한다 (plan B7).
 * 삭제된 댓글은 세지 않는다 (B-51).
 */
public interface PostCommentCount {

    Long getPostId();

    Long getCount();
}
