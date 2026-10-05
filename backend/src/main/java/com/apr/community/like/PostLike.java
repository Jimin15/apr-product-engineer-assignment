package com.apr.community.like;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 누가 어떤 글에 좋아요를 눌렀는지. (post_id, user_id) 는 DB 유니크 제약으로 한 행만 허용된다. */
@Entity
@Table(name = "post_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false, columnDefinition = "TEXT")
    private String userId;

    public static PostLike of(Long postId, String userId) {
        PostLike like = new PostLike();
        like.postId = postId;
        like.userId = userId;
        return like;
    }
}
