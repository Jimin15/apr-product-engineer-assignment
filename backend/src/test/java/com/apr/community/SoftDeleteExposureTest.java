package com.apr.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.apr.community.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * T-70: 모든 조회 경로에서 삭제 데이터가 보이지 않는지 한 곳에서 점검한다 (B-51 · B-64, plan B6-b).
 * 기준값: DB 저장 행 게시글 42 · 댓글 2,076 → API 활성 데이터 게시글 39 · 댓글 1,926.
 */
class SoftDeleteExposureTest extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    @DisplayName("목록 · total · 상세 · 댓글 목록 · 댓글 total · commentCount 어디에도 삭제 데이터가 없다 (39글 · 1,926댓글)")
    void noDeletedDataOnAnyReadPath() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE id <= 42", Long.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE id <= 2076", Long.class)).isEqualTo(2076);

        // 게시글 목록: 39개, 삭제 id 없음, total 39
        JsonNode list = json.readTree(mvc.perform(get("/api/posts?size=100")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        List<Long> postIds = new ArrayList<>();
        list.get("items").forEach(item -> postIds.add(item.get("id").asLong()));
        assertThat(postIds).hasSize(39).doesNotContain(9L, 30L, 31L);
        assertThat(list.get("total").asLong()).isEqualTo(39);

        // commentCount 합 = 1,926 (삭제 댓글 제외, 삭제 글의 댓글은 글이 빠지므로 자연히 제외)
        long commentCountSum = 0;
        for (JsonNode item : list.get("items")) {
            commentCountSum += item.get("commentCount").asLong();
        }
        assertThat(commentCountSum).isEqualTo(1926);

        // 상세: 살아 있는 글은 전부 200, 삭제 글은 404
        for (Long id : postIds) {
            mvc.perform(get("/api/posts/" + id)).andExpect(status().isOk());
        }
        for (long id : List.of(9L, 30L, 31L)) {
            mvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POST_DELETED"));
            mvc.perform(get("/api/posts/" + id + "/comments")).andExpect(status().isNotFound());
        }

        // 댓글 목록 total 합 = 1,926, 어떤 글의 목록에도 삭제된 댓글 id 가 없다
        List<Long> deletedCommentIds = jdbc.queryForList("SELECT id FROM comments WHERE deleted_at IS NOT NULL AND id <= 2076", Long.class);
        long commentTotalSum = 0;
        for (Long id : postIds) {
            JsonNode page = json.readTree(mvc.perform(get("/api/posts/" + id + "/comments?size=100"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            commentTotalSum += page.get("total").asLong();
            page.get("items").forEach(c -> assertThat(deletedCommentIds).doesNotContain(c.get("id").asLong()));
        }
        assertThat(commentTotalSum).isEqualTo(1926);
    }
}
