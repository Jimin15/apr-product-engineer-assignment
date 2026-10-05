package com.apr.community.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.apr.community.support.IntegrationTest;

/** backend spec B-60 ~ B-64. 적재는 컨텍스트 기동 시 SeedLoader 가 이미 수행했다. */
class SeedLoaderTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SeedLoader seedLoader;

    @Test
    @DisplayName("DB 저장 행: 게시글 42 · 댓글 2,076 (삭제 포함), 삭제 표시 3 · 81")
    void storesAllRowsIncludingDeleted() {
        assertThat(count("SELECT COUNT(*) FROM posts WHERE id <= 42")).isEqualTo(42);
        assertThat(count("SELECT COUNT(*) FROM comments WHERE id <= 2076")).isEqualTo(2076);
        assertThat(count("SELECT COUNT(*) FROM posts WHERE id <= 42 AND deleted_at IS NOT NULL")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM comments WHERE id <= 2076 AND deleted_at IS NOT NULL")).isEqualTo(81);
        assertThat(jdbc.queryForList("SELECT id FROM posts WHERE deleted_at IS NOT NULL AND id <= 42 ORDER BY id", Long.class))
                .containsExactly(9L, 30L, 31L);
    }

    @Test
    @DisplayName("삭제된 게시글의 댓글은 건드리지 않는다 — 댓글 자체 미삭제 1,995")
    void doesNotCascadeDeleteToComments() {
        assertThat(count("SELECT COUNT(*) FROM comments WHERE id <= 2076 AND deleted_at IS NULL")).isEqualTo(1995);
        long commentsOfDeletedPosts = count(
                "SELECT COUNT(*) FROM comments c JOIN posts p ON p.id = c.post_id "
                        + "WHERE c.id <= 2076 AND p.deleted_at IS NOT NULL AND c.deleted_at IS NULL");
        assertThat(commentsOfDeletedPosts).isEqualTo(69);
    }

    @Test
    @DisplayName("deleted_at 은 하나의 적재 시각이고 소수점 이하가 없다")
    void deletedAtIsSingleLoadTimeAtSecondPrecision() {
        List<OffsetDateTime> values = jdbc.queryForList(
                "SELECT DISTINCT deleted_at FROM ("
                        + "SELECT deleted_at FROM posts WHERE id <= 42 AND deleted_at IS NOT NULL "
                        + "UNION ALL SELECT deleted_at FROM comments WHERE id <= 2076 AND deleted_at IS NOT NULL) t",
                OffsetDateTime.class);
        assertThat(values).hasSize(1);
        assertThat(values.get(0).getNano()).isZero();
    }

    @Test
    @DisplayName("좋아요 수는 시드 값으로 시작하고 version 은 0")
    void seedsLikeCounts() {
        assertThat(count("SELECT COUNT(*) FROM post_like_counts WHERE post_id <= 42")).isEqualTo(42);
        assertThat(count("SELECT COUNT(*) FROM post_like_counts WHERE post_id <= 42 AND version <> 0")).isZero();
        assertThat(count("SELECT like_count FROM post_like_counts WHERE post_id = 1")).isEqualTo(1796);
        assertThat(count("SELECT COUNT(*) FROM post_likes WHERE post_id <= 42")).isZero();
    }

    @Test
    @DisplayName("다시 실행하면 건너뛰고 행 수가 그대로다")
    void rerunSkips() {
        long postsBefore = count("SELECT COUNT(*) FROM posts");
        long commentsBefore = count("SELECT COUNT(*) FROM comments");

        boolean loaded = seedLoader.load();

        assertThat(loaded).isFalse();
        assertThat(count("SELECT COUNT(*) FROM posts")).isEqualTo(postsBefore);
        assertThat(count("SELECT COUNT(*) FROM comments")).isEqualTo(commentsBefore);
    }

    @Test
    @DisplayName("시퀀스가 시드 최대 id 뒤로 맞춰져 새 행이 충돌 없이 생성된다 (게시글 ≥ 43, 댓글 ≥ 2077)")
    void sequencesContinueAfterSeedIds() {
        Long postId = jdbc.queryForObject(
                "INSERT INTO posts (title, content, author, created_at) VALUES ('seq', 'seq', 'seq_test', now()) RETURNING id",
                Long.class);
        Long commentId = jdbc.queryForObject(
                "INSERT INTO comments (post_id, content, author, created_at) VALUES (?, 'seq', 'seq_test', now()) RETURNING id",
                Long.class, postId);

        assertThat(postId).isGreaterThanOrEqualTo(43L);
        assertThat(commentId).isGreaterThanOrEqualTo(2077L);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
