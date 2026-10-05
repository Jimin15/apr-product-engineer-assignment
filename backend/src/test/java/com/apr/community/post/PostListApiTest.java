package com.apr.community.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.apr.community.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** T-41: B-10 · B-20 ~ B-23 · B-51, Integration C1 · §1.3 */
class PostListApiTest extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    @DisplayName("기본 size 20, total 39 (삭제 제외), 최신순, nextCursor 는 마지막 항목의 (createdAt, id)")
    void firstPage() throws Exception {
        JsonNode page = getPage("/api/posts");

        assertThat(page.get("items")).hasSize(20);
        assertThat(page.get("total").asLong()).isEqualTo(39);
        assertThat(page.get("nextCursor").get("createdAt").asText()).isEqualTo(page.get("items").get(19).get("createdAt").asText());
        assertThat(page.get("nextCursor").get("id").asLong()).isEqualTo(page.get("items").get(19).get("id").asLong());
        assertThat(ids(page)).doesNotContain(9L, 30L, 31L);
        assertThat(page.get("items").get(0).get("isLiked").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("끝까지 이어 받으면 39개, 중복 · 누락 없음, 마지막 nextCursor null")
    void traverseAll() throws Exception {
        List<Long> seen = new ArrayList<>();
        int pages = 0;
        JsonNode page = getPage("/api/posts?size=20");
        while (true) {
            pages++;
            seen.addAll(ids(page));
            if (page.get("nextCursor").isNull()) {
                break;
            }
            page = getPage(next("/api/posts?size=20", page));
        }
        assertThat(pages).isEqualTo(2);
        assertThat(seen).hasSize(39);
        assertThat(new LinkedHashSet<>(seen)).hasSize(39);
        assertThat(jdbc.queryForList(
                "SELECT id FROM posts WHERE deleted_at IS NULL AND id <= 42 ORDER BY created_at DESC, id DESC", Long.class))
                .containsExactlyElementsOf(seen);
    }

    @Test
    @DisplayName("같은 시각 항목이 묶음 경계에 걸려도 중복 · 누락 없음 (id 로 순서 고정)")
    void sameCreatedAtAcrossBoundary() throws Exception {
        for (int i = 0; i < 3; i++) {
            jdbc.update("INSERT INTO posts (title, content, author, created_at) VALUES (?, 'c', 'tie_test', '2030-01-01T00:00:00Z')", "tie" + i);
        }
        List<Long> seen = new ArrayList<>();
        JsonNode page = getPage("/api/posts?size=1");
        while (seen.size() < 4) {
            seen.addAll(ids(page));
            page = getPage(next("/api/posts?size=1", page));
        }
        List<Long> tieIds = jdbc.queryForList("SELECT id FROM posts WHERE author = 'tie_test' ORDER BY id DESC", Long.class);
        assertThat(seen.subList(0, 3)).containsExactlyElementsOf(tieIds);
        assertThat(seen.get(3)).isLessThanOrEqualTo(42L);
    }

    @Test
    @DisplayName("첫 묶음을 받은 뒤 새 글이 생겨도 다음 묶음에 중복이 없다")
    void newPostBetweenPages() throws Exception {
        JsonNode first = getPage("/api/posts?size=20");
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 글\",\"content\":\"본문\"}"))
                .andExpect(status().isCreated());
        JsonNode second = getPage(next("/api/posts?size=20", first));

        Set<Long> firstIds = new LinkedHashSet<>(ids(first));
        assertThat(ids(second)).doesNotContainAnyElementsOf(firstIds);
        assertThat(ids(second)).allMatch(id -> id <= 42);
        assertThat(second.get("total").asLong()).isEqualTo(40);
    }

    @Test
    @DisplayName("size 1 · 100 정상")
    void sizeBounds() throws Exception {
        assertThat(getPage("/api/posts?size=1").get("items")).hasSize(1);
        assertThat(getPage("/api/posts?size=100").get("items")).hasSize(39);
        assertThat(getPage("/api/posts?size=100").get("nextCursor").isNull()).isTrue();
    }

    @Test
    @DisplayName("size 0 · 101 · 문자, 커서 한쪽만, 잘못된 커서 형식 → 400 INVALID_QUERY")
    void invalidQuery() throws Exception {
        for (String q : List.of("size=0", "size=101", "size=abc",
                "cursorId=5", "cursorCreatedAt=2026-08-30T13:55:54Z",
                "cursorCreatedAt=not-a-date&cursorId=5", "cursorCreatedAt=2026-08-30T13:55:54Z&cursorId=abc",
                "cursorCreatedAt=2026-08-30T13:55:54Z&cursorId=0")) {
            mvc.perform(get("/api/posts?" + q))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        }
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
