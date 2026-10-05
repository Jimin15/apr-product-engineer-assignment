package com.apr.community.post;

/**
 * 게시글별 좋아요 수 조회용 프로젝션.
 * PostLikeCount 엔티티를 그대로 반환하면 Spring Data 가 도메인 타입(Post)과 달라 DTO 생성자 식으로 재작성하므로,
 * 필요한 두 값만 인터페이스 프로젝션으로 받는다 (plan B10-c).
 */
public interface PostLikeCountView {

    Long getPostId();

    Integer getLikeCount();
}
