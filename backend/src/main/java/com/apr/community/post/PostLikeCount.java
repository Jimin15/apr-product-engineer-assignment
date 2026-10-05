package com.apr.community.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글의 좋아요 카운터 (게시글당 1행). 게시글 행과 분리해 좋아요 토글의 낙관적 락(version)이 글 수정과 충돌하지 않게 한다 (plan B8-a).
 * 게시글이 생성될 때 0으로 함께 만들어지므로 post 패키지가 소유한다. 증감은 like 기능이 한다.
 * version 은 래퍼 타입이다: 새 객체는 null 이라 persist 로 처리되고, DB 에서 읽은 행은 항상 값(기본 0)을 갖는다.
 */
@Entity
@Table(name = "post_like_counts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeCount {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Version
    @Column(nullable = false)
    private Long version;

    public static PostLikeCount of(Long postId, int likeCount) {
        PostLikeCount count = new PostLikeCount();
        count.postId = postId;
        count.likeCount = likeCount;
        return count;
    }

    public void increase() {
        this.likeCount++;
    }

    public void decrease() {
        this.likeCount--;
    }
}
