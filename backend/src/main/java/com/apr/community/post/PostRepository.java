package com.apr.community.post;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    long countByDeletedAtIsNull();

    /** 목록 첫 묶음. 최신순 (created_at DESC, id DESC) — 같은 시각은 id 로 순서를 고정한다 (B-23). */
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findFirstPage(Limit limit);

    /** 커서 (createdAt, id) 보다 뒤(더 오래된) 항목부터 (plan B9). */
    @Query("""
            SELECT p FROM Post p
            WHERE p.deletedAt IS NULL
              AND (p.createdAt < :createdAt OR (p.createdAt = :createdAt AND p.id < :id))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Post> findPageAfter(@Param("createdAt") Instant createdAt, @Param("id") Long id, Limit limit);

    // ---- 화면 응답용 집계. 다른 기능의 테이블을 조회 쿼리에서 직접 참조한다 (plan B10-c). 페이지 단위로 한 번에 묶어 N+1 을 피한다 (B7).

    @Query("SELECT plc.postId AS postId, plc.likeCount AS likeCount FROM PostLikeCount plc WHERE plc.postId IN :postIds")
    List<PostLikeCountView> findLikeCounts(@Param("postIds") Collection<Long> postIds);

    @Query("""
            SELECT c.postId AS postId, COUNT(c) AS count
            FROM Comment c
            WHERE c.postId IN :postIds AND c.deletedAt IS NULL
            GROUP BY c.postId
            """)
    List<PostCommentCount> countActiveComments(@Param("postIds") Collection<Long> postIds);

    @Query("SELECT pl.postId FROM PostLike pl WHERE pl.userId = :userId AND pl.postId IN :postIds")
    Set<Long> findLikedPostIds(@Param("userId") String userId, @Param("postIds") Collection<Long> postIds);
}
