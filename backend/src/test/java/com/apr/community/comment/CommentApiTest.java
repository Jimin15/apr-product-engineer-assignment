package com.apr.community.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.apr.community.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** T-50 · T-51 · T-52: B-16 ~ B-18 · B-20 ~ B-24 · B-30 · B-32 · B-33 · B-50 ~ B-52, Integration C5 */
class CommentApiTest extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---- T-50 목록

    @Test
    @DisplayName("2번 글: total 1,411, size=2 로 최초 2개 → 커서로 이어 받기, 삭제된 댓글 제외, 최신순")
    void listWithCursor() throws Exception {
        JsonNode first = getPage("/api/posts/2/comments?size=2");
        assertThat(first.get("items")).hasSize(2);
        assertThat(first.get("total").asLong()).isEqualTo(1411);
        assertThat(first.get("items").get(0).get("postId").asLong()).isEqualTo(2);
        assertThat(first.get("nextCursor").isNull()).isFalse();

        JsonNode second = getPage(next("/api/posts/2/comments?size=20", first));
        assertThat(second.get("items")).hasSize(20);
        assertThat(ids(second)).doesNotContainAnyElementsOf(ids(first));

        List<Long> expected = jdbc.queryForList(
                "SELECT id FROM comments WHERE post_id = 2 AND deleted_at IS NULL ORDER BY created_at DESC, id DESC LIMIT 22",
                Long.class);
        List<Long> actual = new ArrayList<>(ids(first));
        actual.addAll(ids(second));
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("같은 초에 작성된 시드 댓글이 경계에 걸려도 중복 · 누락 없이 끝까지 순회 (2번 글 1,411개, size 100)")
    void traverseAllOfPost2() throws Exception {
        List<Long> seen = new ArrayList<>();
        JsonNode page = getPage("/api/posts/2/comments?size=100");
        while (true) {
            seen.addAll(ids(page));
            if (page.get("nextCursor").isNull()) {
                break;
            }
            page = getPage(next("/api/posts/2/comments?size=100", page));
        }
        assertThat(seen).hasSize(1411);
        assertThat(new LinkedHashSet<>(seen)).hasSize(1411);
    }

    @Test
    @DisplayName("댓글 없는 글(10) → items 빈 배열, total 0, nextCursor null")
    void emptyList() throws Exception {
        mvc.perform(get("/api/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.nextCursor").value((Object) null));
    }

    @Test
    @DisplayName("삭제된 게시글(9)의 댓글 목록 → 404 POST_DELETED, 없는 게시글 → 404 POST_NOT_FOUND")
    void listOfDeletedOrMissingPost() throws Exception {
        mvc.perform(get("/api/posts/9/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
        mvc.perform(get("/api/posts/999999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("목록은 요청자를 쓰지 않으므로 헤더 · 쿠키가 달라도 200 (B11-a); 잘못된 size 는 400")
    void listIgnoresRequesterMismatch() throws Exception {
        mvc.perform(get("/api/posts/1/comments").header("x-user-id", "Jelin").cookie(new Cookie("x-user-id", "Roidl_08")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/1/comments?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
    }

    // ---- T-51 작성

    @Test
    @DisplayName("작성 → 201, 내용 그대로, 작성자 = 요청자, 목록 맨 앞 · commentCount +1")
    void create() throws Exception {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id = 10 AND deleted_at IS NULL", Long.class);

        mvc.perform(post("/api/posts/10/comments").header("x-user-id", "cos_holic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  첫 댓글\\n줄바꿈  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(10))
                .andExpect(jsonPath("$.content").value("  첫 댓글\n줄바꿈  "))
                .andExpect(jsonPath("$.author").value("cos_holic"))
                .andExpect(jsonPath("$.id").isNumber());

        mvc.perform(get("/api/posts/10/comments"))
                .andExpect(jsonPath("$.total").value(before + 1))
                .andExpect(jsonPath("$.items[0].author").value("cos_holic"));
        mvc.perform(get("/api/posts/10"))
                .andExpect(jsonPath("$.commentCount").value(before + 1));
    }

    @Test
    @DisplayName("공백만 → 400 INVALID_INPUT(content), 삭제된 게시글 → 404 POST_DELETED")
    void createInvalidOrDeletedPost() throws Exception {
        mvc.perform(post("/api/posts/10/comments").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\" \\n \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("content"));
        mvc.perform(post("/api/posts/9/comments").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
    }

    // ---- T-52 삭제

    @Test
    @DisplayName("본인 댓글 삭제 → 204, DB 행 유지, 목록 · commentCount 에서 빠짐")
    void deleteOwn() throws Exception {
        long id = createComment(10, "Roidl_08", "지울 댓글");
        long countBefore = jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id = 10 AND deleted_at IS NULL", Long.class);

        mvc.perform(delete("/api/comments/" + id).header("x-user-id", "Roidl_08"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NOT NULL", Long.class, id)).isEqualTo(1);
        mvc.perform(get("/api/posts/10/comments")).andExpect(jsonPath("$.total").value(countBefore - 1));
        mvc.perform(get("/api/posts/10")).andExpect(jsonPath("$.commentCount").value(countBefore - 1));
    }

    @Test
    @DisplayName("남의 댓글 → 403 NOT_OWNER, 없는 댓글 → 404 COMMENT_NOT_FOUND, 삭제된 댓글 → 404 COMMENT_DELETED")
    void deleteOthersMissingDeleted() throws Exception {
        long id = createComment(10, "Roidl_08", "남의 댓글");
        mvc.perform(delete("/api/comments/" + id).header("x-user-id", "Jelin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));

        mvc.perform(delete("/api/comments/999999").header("x-user-id", "Jelin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));

        Long deletedSeedComment = jdbc.queryForObject(
                "SELECT c.id FROM comments c JOIN posts p ON p.id = c.post_id WHERE c.deleted_at IS NOT NULL AND p.deleted_at IS NULL ORDER BY c.id LIMIT 1",
                Long.class);
        String author = jdbc.queryForObject("SELECT author FROM comments WHERE id = ?", String.class, deletedSeedComment);
        mvc.perform(delete("/api/comments/" + deletedSeedComment).header("x-user-id", author))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_DELETED"));
    }

    @Test
    @DisplayName("삭제된 게시글(9)에 달린 살아 있는 댓글을 삭제 → 404 POST_DELETED (부모 게시글 확인이 먼저)")
    void deleteCommentOfDeletedPost() throws Exception {
        Long id = jdbc.queryForObject("SELECT id FROM comments WHERE post_id = 9 AND deleted_at IS NULL ORDER BY id LIMIT 1", Long.class);
        String author = jdbc.queryForObject("SELECT author FROM comments WHERE id = ?", String.class, id);

        mvc.perform(delete("/api/comments/" + id).header("x-user-id", author))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE id = ? AND deleted_at IS NULL", Long.class, id)).isEqualTo(1);
    }

    private long createComment(long postId, String user, String content) throws Exception {
        String body = mvc.perform(post("/api/posts/" + postId + "/comments").header("x-user-id", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private JsonNode getPage(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static String next(String base, JsonNode page) {
        JsonNode cursor = page.get("nextCursor");
        return base + "&cursorCreatedAt=" + cursor.get("createdAt").asText() + "&cursorId=" + cursor.get("id").asLong();
    }

    private static List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }
}
