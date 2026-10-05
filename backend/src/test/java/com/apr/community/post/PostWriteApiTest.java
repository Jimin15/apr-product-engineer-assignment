package com.apr.community.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.apr.community.support.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** T-42 · T-43 · T-44: B-11 · B-13 · B-14 · B-30 ~ B-33 · B-50 ~ B-52, Integration C2 */
class PostWriteApiTest extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---- T-42 작성

    @Test
    @DisplayName("작성 → 201, 작성자 = 요청자, 제목 trim 저장, 내용 그대로, 카운터 0, 시각 초 단위")
    void create() throws Exception {
        String body = mvc.perform(post("/api/posts").header("x-user-id", "Jelin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "  새 글 제목  ", "content", "  본문\n그대로  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", greaterThanOrEqualTo(43)))
                .andExpect(jsonPath("$.title").value("새 글 제목"))
                .andExpect(jsonPath("$.content").value("  본문\n그대로  "))
                .andExpect(jsonPath("$.author").value("Jelin"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andReturn().getResponse().getContentAsString();

        long id = json.readTree(body).get("id").asLong();
        OffsetDateTime createdAt = jdbc.queryForObject("SELECT created_at FROM posts WHERE id = ?", OffsetDateTime.class, id);
        assertThat(createdAt.getNano()).isZero();
        assertThat(jdbc.queryForObject("SELECT like_count FROM post_like_counts WHERE post_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM post_like_counts WHERE post_id = ?", Long.class, id)).isZero();

        mvc.perform(get("/api/posts/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("새 글 제목"));
    }

    @Test
    @DisplayName("제목 · 내용 둘 다 공백 → 400 INVALID_INPUT, errors 2개 (필드명 순)")
    void createBothBlank() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"content\":\"\\n\\t\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("content"))
                .andExpect(jsonPath("$.errors[1].field").value("title"));
    }

    @Test
    @DisplayName("제목 21 코드포인트 → 400, title 만 실패 / 20 코드포인트(이모지 포함) → 201")
    void createTitleLength() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "가".repeat(21), "content", "본문"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "🙏🏻" + "가".repeat(18), "content", "본문"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내용 누락 → 400 INVALID_INPUT")
    void createMissingContent() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    // ---- T-43 수정

    @Test
    @DisplayName("본인 글 수정 → 200 + 수정된 게시글, 상세에도 반영")
    void updateOwn() throws Exception {
        long id = createPost("Jelin", "원래 제목", "원래 내용");

        mvc.perform(patch("/api/posts/" + id).header("x-user-id", "Jelin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", " 바뀐 제목 ", "content", "바뀐 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.content").value("바뀐 내용"));

        mvc.perform(get("/api/posts/" + id))
                .andExpect(jsonPath("$.title").value("바뀐 제목"));
    }

    @Test
    @DisplayName("남의 글 수정 → 403 NOT_OWNER, 데이터 불변")
    void updateOthers() throws Exception {
        mvc.perform(patch("/api/posts/1").header("x-user-id", "Jelin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "해킹", "content", "해킹"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));

        assertThat(jdbc.queryForObject("SELECT title FROM posts WHERE id = 1", String.class)).isEqualTo("부스터 프로 써보신 분?");
    }

    @Test
    @DisplayName("삭제된 글 수정 → 404 POST_DELETED (권한보다 삭제 확인이 먼저)")
    void updateDeleted() throws Exception {
        mvc.perform(patch("/api/posts/9").header("x-user-id", "someone_else")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "x", "content", "x"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
    }

    @Test
    @DisplayName("수정 검증 실패 → 400 INVALID_INPUT")
    void updateInvalid() throws Exception {
        long id = createPost("Jelin", "제목", "내용");
        mvc.perform(patch("/api/posts/" + id).header("x-user-id", "Jelin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // ---- T-44 삭제

    @Test
    @DisplayName("본인 글 삭제 → 204, 목록 · 상세에서 사라짐, DB 행은 남고 댓글 행은 그대로")
    void deleteOwn() throws Exception {
        long id = createPost("Jelin", "지울 글", "내용");
        jdbc.update("INSERT INTO comments (post_id, content, author, created_at) VALUES (?, 'c', 'Roidl_08', now())", id);

        mvc.perform(delete("/api/posts/" + id).header("x-user-id", "Jelin"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(get("/api/posts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
        mvc.perform(get("/api/posts?size=100"))
                .andExpect(jsonPath("$.total").value(39))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE id = ? AND deleted_at IS NOT NULL", Long.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted_at IS NULL", Long.class, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("남의 글 삭제 → 403, 삭제된 글 재삭제 → 404 POST_DELETED")
    void deleteOthersAndDeleted() throws Exception {
        mvc.perform(delete("/api/posts/1").header("x-user-id", "Jelin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM posts WHERE id = 1", OffsetDateTime.class)).isNull();

        mvc.perform(delete("/api/posts/9").header("x-user-id", "apr_tester"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"));
    }

    private long createPost(String user, String title, String content) throws Exception {
        String body = mvc.perform(post("/api/posts").header("x-user-id", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
}
