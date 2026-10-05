package com.apr.community.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.apr.community.support.IntegrationTest;

/** T-40: B-12 · B-33 · B-34 · B-51, Integration C2-b. 시드 기준. */
class PostDetailApiTest extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("상세: 시드 1번 — 예시 형태 그대로, likeCount 시드값, isLiked 기본 false, 시각 초 단위")
    void detailOfSeedPost() throws Exception {
        mvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("부스터 프로 써보신 분?"))
                .andExpect(jsonPath("$.author").value("apr_tester"))
                .andExpect(jsonPath("$.likeCount").value(1796))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.commentCount").isNumber())
                .andExpect(jsonPath("$.createdAt").value("2026-08-30T13:55:54Z"))
                .andExpect(jsonPath("$.liked").doesNotExist());
    }

    @Test
    @DisplayName("commentCount 는 삭제되지 않은 댓글 수 — 2번 1,411, 10번 0")
    void commentCountExcludesDeleted() throws Exception {
        mvc.perform(get("/api/posts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1411));
        mvc.perform(get("/api/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(0));
    }

    @Test
    @DisplayName("삭제된 게시글(9) → 404 POST_DELETED")
    void deletedPost() throws Exception {
        mvc.perform(get("/api/posts/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
    }

    @Test
    @DisplayName("없는 게시글 → 404 POST_NOT_FOUND")
    void missingPost() throws Exception {
        mvc.perform(get("/api/posts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("상세는 요청자를 쓰므로 헤더 · 쿠키 불일치면 400 (B11-a)")
    void mismatchedRequester() throws Exception {
        mvc.perform(get("/api/posts/1").header("x-user-id", "Jelin").cookie(new Cookie("x-user-id", "Roidl_08")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ID_MISMATCH"));
    }
}
