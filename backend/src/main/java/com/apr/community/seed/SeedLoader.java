package com.apr.community.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 기동 시 seed.json 을 DB 에 적재한다 (backend spec B-60 ~ B-64, plan B3 · B4 · B5).
 *
 * - 실행 시점: 모든 싱글톤이 준비된 직후, 내장 웹 서버가 포트를 열기 전 (SmartInitializingSingleton).
 *   적재가 끝나야 요청을 받으므로 적재 중 /health · API 가 호출되는 상황이 생기지 않는다.
 * - 멱등성: posts 가 비어 있을 때만 적재한다. 소프트 삭제라 한 번 적재되면 다시 비지 않는다.
 * - 원자성: 전체 적재를 하나의 트랜잭션으로 묶는다. 실패하면 롤백되고 예외로 기동이 실패한다.
 * - id 는 시드 값을 그대로 INSERT 하고, 마지막에 시퀀스를 각 테이블 최대 id 로 맞춘다.
 * - deletedPostIds · deletedCommentIds 에 있는 행만 deleted_at 을 기록한다. 값은 적재 시각(초 단위)이다.
 *   삭제된 게시글의 댓글은 건드리지 않는다 — 부모 게시글 상태로 걸러진다 (plan B6-c).
 */
@Slf4j
@Component
public class SeedLoader implements SmartInitializingSingleton {

    private static final String INSERT_POST =
            "INSERT INTO posts (id, title, content, author, created_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String INSERT_LIKE_COUNT =
            "INSERT INTO post_like_counts (post_id, like_count, version) VALUES (?, ?, 0)";
    private static final String INSERT_COMMENT =
            "INSERT INTO comments (id, post_id, content, author, created_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String RESET_SEQUENCE =
            "SELECT setval(pg_get_serial_sequence(?, 'id'), (SELECT MAX(id) FROM %s))";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Path seedPath;

    public SeedLoader(JdbcTemplate jdbc,
                      PlatformTransactionManager transactionManager,
                      ObjectMapper objectMapper,
                      Clock clock,
                      @Value("${app.seed-path}") String seedPath) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.seedPath = Path.of(seedPath);
    }

    @Override
    public void afterSingletonsInstantiated() {
        load();
    }

    /** @return 적재했으면 true, 이미 데이터가 있어 건너뛰었으면 false */
    public boolean load() {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            // 1. 이미 적재돼 있으면 건너뛴다 (재기동 시 중복 적재 방지)
            long existing = jdbc.queryForObject("SELECT COUNT(*) FROM posts", Long.class);
            if (existing > 0) {
                log.info("Seed skipped: posts already has {} rows", existing);
                return false;
            }

            // 2. seed.json 읽기, 삭제 표시 시각(적재 시각) · 삭제 대상 id 준비
            SeedData data = read();
            Instant loadedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
            Set<Long> deletedPosts = Set.copyOf(data.deletedPostIds());
            Set<Long> deletedComments = Set.copyOf(data.deletedCommentIds());

            // 3. 게시글 → 좋아요 카운터 → 댓글 순으로 시드 id 그대로 INSERT (외래 키 순서)
            insertPosts(data.posts(), deletedPosts, loadedAt);
            insertLikeCounts(data.posts());
            insertComments(data.comments(), deletedComments, loadedAt);
            // 4. 새로 작성되는 글 · 댓글의 id 가 시드 다음 번호부터 나오도록 시퀀스를 맞춘다
            resetSequence("posts");
            resetSequence("comments");

            log.info("Seed loaded from {}: posts={} (deleted {}), comments={} (deleted {}), deletedAt={}",
                    seedPath, data.posts().size(), deletedPosts.size(),
                    data.comments().size(), deletedComments.size(), loadedAt);
            return true;
        }));
    }

    private SeedData read() {
        try (InputStream in = Files.newInputStream(seedPath)) {
            return objectMapper.readValue(in, SeedData.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read seed file: " + seedPath.toAbsolutePath(), e);
        }
    }

    private void insertPosts(List<SeedData.SeedPost> posts, Set<Long> deleted, Instant loadedAt) {
        List<Object[]> rows = posts.stream()
                .map(p -> new Object[]{p.id(), p.title(), p.content(), p.author(),
                        utc(p.createdAt()), deleted.contains(p.id()) ? utc(loadedAt) : null})
                .toList();
        jdbc.batchUpdate(INSERT_POST, rows, new int[]{
                Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                Types.TIMESTAMP_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE});
    }

    private void insertLikeCounts(List<SeedData.SeedPost> posts) {
        List<Object[]> rows = posts.stream()
                .map(p -> new Object[]{p.id(), p.likeCount()})
                .toList();
        jdbc.batchUpdate(INSERT_LIKE_COUNT, rows, new int[]{Types.BIGINT, Types.INTEGER});
    }

    private void insertComments(List<SeedData.SeedComment> comments, Set<Long> deleted, Instant loadedAt) {
        List<Object[]> rows = comments.stream()
                .map(c -> new Object[]{c.id(), c.postId(), c.content(), c.author(),
                        utc(c.createdAt()), deleted.contains(c.id()) ? utc(loadedAt) : null})
                .toList();
        jdbc.batchUpdate(INSERT_COMMENT, rows, new int[]{
                Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR,
                Types.TIMESTAMP_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE});
    }

    /** id 를 지정해 넣었으므로 IDENTITY 시퀀스를 최대 id 로 맞춘다. 다음 생성 id 는 max+1 이 된다. */
    private void resetSequence(String table) {
        jdbc.queryForObject(RESET_SEQUENCE.formatted(table), Long.class, table);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
