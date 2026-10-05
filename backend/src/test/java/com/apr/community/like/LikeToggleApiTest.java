package com.apr.community.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.like.dto.LikeResponse;
import com.apr.community.support.IntegrationTest;

/** T-60: B-15 · B-40 ~ B-43 · B-33, product D7 */
class LikeToggleApiTest extends IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(LikeToggleApiTest.class);

    @Autowired
    MockMvc mvc;

    @Autowired
    LikeFacade likeFacade;

    @Test
    @DisplayName("시드 1번: 1796 · false → 토글 1797 · true → 토글 1796 · false, 상세 isLiked 도 함께 바뀜")
    void toggleTwiceOnSeedPost() throws Exception {
        mvc.perform(get("/api/posts/1").header("x-user-id", "Jelin"))
                .andExpect(jsonPath("$.likeCount").value(1796))
                .andExpect(jsonPath("$.isLiked").value(false));

        mvc.perform(post("/api/posts/1/likes/toggle").header("x-user-id", "Jelin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1797));
        mvc.perform(get("/api/posts/1").header("x-user-id", "Jelin"))
                .andExpect(jsonPath("$.likeCount").value(1797))
                .andExpect(jsonPath("$.isLiked").value(true));
        mvc.perform(get("/api/posts/1").header("x-user-id", "Roidl_08"))
                .andExpect(jsonPath("$.isLiked").value(false));
        mvc.perform(get("/api/posts?size=1"))
                .andExpect(jsonPath("$.items[0].likeCount").isNumber());

        mvc.perform(post("/api/posts/1/likes/toggle").header("x-user-id", "Jelin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.likeCount").value(1796));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id = 1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM post_like_counts WHERE post_id = 1", Long.class)).isEqualTo(2L);
    }

    @Test
    @DisplayName("삭제된 게시글(9) → 404 POST_DELETED, 없는 게시글 → 404 POST_NOT_FOUND")
    void deletedOrMissingPost() throws Exception {
        mvc.perform(post("/api/posts/9/likes/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
        mvc.perform(post("/api/posts/999999/likes/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("같은 사용자 동시 요청 2개 → 둘 다 성공하면 원래 상태(기록 0 · 숫자 원래값), 어떤 경우에도 숫자 = 기록 수")
    void sameUserConcurrentToggles() throws Exception {
        long postId = createPost();
        List<Outcome> outcomes = runConcurrently(2, i -> likeFacade.toggle(postId, "same_user"));

        long rows = jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id = ?", Long.class, postId);
        int count = jdbc.queryForObject("SELECT like_count FROM post_like_counts WHERE post_id = ?", Integer.class, postId);
        log.info("same-user x2: outcomes={}, rows={}, count={}", outcomes, rows, count);

        assertThat(count).isEqualTo(rows);
        assertThat(count).isBetween(0, 1);
        if (outcomes.stream().allMatch(Outcome::succeeded)) {
            assertThat(count).isZero();
            assertThat(outcomes.stream().filter(o -> o.response().isLiked()).count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("누른 상태에서 같은 사용자 동시 취소 2개 → 둘 다 성공하면 취소 → 좋아요 순서로 최종 기록 1 · 숫자 1 (version 충돌이 잡는 케이스)")
    void sameUserConcurrentUnlikes() throws Exception {
        long postId = createPost();
        jdbc.update("INSERT INTO post_likes (post_id, user_id) VALUES (?, 'same_user')", postId);
        jdbc.update("UPDATE post_like_counts SET like_count = 1 WHERE post_id = ?", postId);

        List<Outcome> outcomes = runConcurrently(2, i -> likeFacade.toggle(postId, "same_user"));

        long rows = jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id = ?", Long.class, postId);
        int count = jdbc.queryForObject("SELECT like_count FROM post_like_counts WHERE post_id = ?", Integer.class, postId);
        log.info("same-user unlike x2: outcomes={}, rows={}, count={}", outcomes, rows, count);

        assertThat(count).isEqualTo(rows);
        assertThat(count).isBetween(0, 1);
        if (outcomes.stream().allMatch(Outcome::succeeded)) {
            assertThat(count).isEqualTo(1);
            assertThat(outcomes.stream().filter(o -> !o.response().isLiked()).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(o -> o.response().isLiked()).count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("서로 다른 사용자 4명 동시 요청 → 숫자 = 성공한 요청 수 = 기록 수, 중복 · 음수 없음")
    void differentUsersConcurrentToggles() throws Exception {
        long postId = createPost();
        int users = 4;
        List<Outcome> outcomes = runConcurrently(users, i -> likeFacade.toggle(postId, "user_" + i));

        long succeeded = outcomes.stream().filter(Outcome::succeeded).count();
        long rows = jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id = ?", Long.class, postId);
        int count = jdbc.queryForObject("SELECT like_count FROM post_like_counts WHERE post_id = ?", Integer.class, postId);
        log.info("different-users x{}: succeeded={}, rows={}, count={}, failures={}", users, succeeded, rows, count,
                outcomes.stream().filter(o -> !o.succeeded()).map(o -> o.error().code()).toList());

        // 몇 명이 성공하는지는 확률적(낙관적 락 + 3회 상한)이라 단언하지 않는다. 불변식만 확인하고 관측값은 로그 · tasks 에 기록한다.
        assertThat(count).isEqualTo(rows).isEqualTo((int) succeeded);
        assertThat(succeeded).isGreaterThanOrEqualTo(1);
        assertThat(outcomes.stream().filter(o -> !o.succeeded()).map(o -> o.error().code()))
                .allMatch(code -> code == ErrorCode.CONCURRENT_UPDATE);   // 실패가 있다면 전부 409 (전원 성공도 정상)
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT user_id) FROM post_likes WHERE post_id = ?", Long.class, postId))
                .isEqualTo(rows);
    }

    // ---- helpers

    private record Outcome(LikeResponse response, ApiException error) {
        boolean succeeded() {
            return response != null;
        }
    }

    private interface Toggle {
        LikeResponse run(int index);
    }

    private List<Outcome> runConcurrently(int n, Toggle toggle) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int index = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    return new Outcome(toggle.run(index), null);
                } catch (ApiException e) {
                    return new Outcome(null, e);
                }
            }));
        }
        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> f : futures) {
            outcomes.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return outcomes;
    }

    private long createPost() {
        Long id = jdbc.queryForObject(
                "INSERT INTO posts (title, content, author, created_at) VALUES ('like', 'like', 'like_test', now()) RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO post_like_counts (post_id, like_count, version) VALUES (?, 0, 0)", id);
        return id;
    }
}
