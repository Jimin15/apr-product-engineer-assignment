package com.apr.community.comment;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    long countByPostIdAndDeletedAtIsNull(Long postId);

    /** 댓글 첫 묶음. 최신순 (created_at DESC, id DESC). 시드에 같은 초의 댓글이 있어 id 로 순서를 고정한다 (B-23). */
    @Query("""
            SELECT c FROM Comment c
            WHERE c.postId = :postId AND c.deletedAt IS NULL
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Comment> findFirstPage(@Param("postId") Long postId, Limit limit);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.postId = :postId AND c.deletedAt IS NULL
              AND (c.createdAt < :createdAt OR (c.createdAt = :createdAt AND c.id < :id))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Comment> findPageAfter(@Param("postId") Long postId,
                                @Param("createdAt") Instant createdAt,
                                @Param("id") Long id,
                                Limit limit);
}
