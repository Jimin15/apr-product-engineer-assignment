package com.apr.community.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 실제 PostgreSQL 로 도는 통합 테스트의 공통 베이스 (plan B16 · B16-a).
 * 컨테이너는 테스트 JVM 당 하나를 띄워 모든 통합 테스트 클래스가 공유한다 (매 클래스마다 띄우면 느리다).
 *
 * 데이터 규칙: 시드(게시글 ≤ 42, 댓글 ≤ 2076)는 읽기 전용으로 두고, 쓰기 · 삭제 테스트는 자기가 만든 행에만 한다.
 * 각 테스트 뒤에 시드 밖의 행과 좋아요 기록을 지워 시드 상태로 되돌린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    public static final long SEED_MAX_POST_ID = 42;
    public static final long SEED_MAX_COMMENT_ID = 2076;

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @AfterEach
    void restoreSeedState() {
        jdbc.update("DELETE FROM post_likes");
        jdbc.update("DELETE FROM comments WHERE id > ?", SEED_MAX_COMMENT_ID);
        jdbc.update("DELETE FROM post_like_counts WHERE post_id > ?", SEED_MAX_POST_ID);
        jdbc.update("DELETE FROM posts WHERE id > ?", SEED_MAX_POST_ID);
        // 시드 글에 좋아요를 토글했다 되돌리는 테스트는 like_count 는 복원하지만 version 은 올라가므로 함께 되돌린다.
        jdbc.update("UPDATE post_like_counts SET version = 0 WHERE post_id <= ? AND version <> 0", SEED_MAX_POST_ID);
    }
}
