package com.apr.community.common;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.common.user.CurrentUser;
import com.apr.community.common.validation.CodePointLength;

/**
 * 공통 웹 계층 검증 (DB 없음): 오류 응답 형태(T-20), @CurrentUser 주입과 쿠키 디코딩(T-21), 검증 애너테이션 연동(T-22).
 * 실제 API 컨트롤러 대신 테스트 전용 ProbeController 로 각 경로를 만든다.
 */
@WebMvcTest(controllers = CommonWebLayerTest.ProbeController.class)
@Import(CommonWebLayerTest.ProbeController.class)
class CommonWebLayerTest {

    @Autowired
    MockMvc mvc;

    @RestController
    static class ProbeController {

        record Body(@NotBlank(message = "제목을 입력해 주세요.") @CodePointLength(max = 20, message = "제목은 20자 이하여야 합니다.") String title,
                    @NotBlank(message = "내용을 입력해 주세요.") String content) {}

        @GetMapping("/probe/me")
        String me(@CurrentUser String userId) {
            return userId;
        }

        @GetMapping("/probe/deleted")
        void deleted() {
            throw new ApiException(ErrorCode.POST_DELETED);
        }

        @PostMapping("/probe/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/probe/query")
        String query(@RequestParam int size) {
            return String.valueOf(size);
        }

        @GetMapping("/probe/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/probe/path/{id}")
        String path(@PathVariable Long id) {
            return String.valueOf(id);
        }
    }

    @Test
    @DisplayName("디코딩할 수 없는 쿠키 값 → 400 INVALID_INPUT (500 아님)")
    void currentUserUndecodableCookie() throws Exception {
        mvc.perform(get("/probe/me").cookie(new Cookie("x-user-id", "%ZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("경로 변수 타입 불일치 (/posts/abc 꼴) → 400 INVALID_INPUT, 쿼리 파라미터와 구분")
    void pathVariableTypeMismatch() throws Exception {
        mvc.perform(get("/probe/path/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("경로의 id 형식이 올바르지 않습니다."));
    }

    // ---- T-21 @CurrentUser

    @Test
    @DisplayName("헤더로 요청자 주입")
    void currentUserFromHeader() throws Exception {
        mvc.perform(get("/probe/me").header("x-user-id", "Jelin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Jelin"));
    }

    @Test
    @DisplayName("쿠키는 URL 디코딩해서 읽는다 (dev-user 가 encodeURIComponent 로 저장)")
    void currentUserFromEncodedCookie() throws Exception {
        mvc.perform(get("/probe/me").cookie(new Cookie("x-user-id", "skin%5Fbeginner")))
                .andExpect(status().isOk())
                .andExpect(content().string("skin_beginner"));
    }

    @Test
    @DisplayName("헤더 · 쿠키 불일치 → 400 USER_ID_MISMATCH")
    void currentUserMismatch() throws Exception {
        mvc.perform(get("/probe/me").header("x-user-id", "Jelin").cookie(new Cookie("x-user-id", "Roidl_08")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_ID_MISMATCH"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("둘 다 없으면 apr_tester")
    void currentUserDefault() throws Exception {
        mvc.perform(get("/probe/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("apr_tester"));
    }

    // ---- T-20 오류 응답

    @Test
    @DisplayName("ApiException → 코드의 상태와 { code, message }, errors 없음")
    void apiException() throws Exception {
        mvc.perform(get("/probe/deleted"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_DELETED"))
                .andExpect(jsonPath("$.message").value("삭제된 게시글입니다."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("검증 실패 → 400 INVALID_INPUT + 필드별 errors (필드명 순)")
    void validationBothBlank() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"content\":\"\\n\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("content"))
                .andExpect(jsonPath("$.errors[0].message").value("내용을 입력해 주세요."))
                .andExpect(jsonPath("$.errors[1].field").value("title"))
                .andExpect(jsonPath("$.errors[1].message").value("제목을 입력해 주세요."));
    }

    @Test
    @DisplayName("제목 21 코드포인트 → INVALID_INPUT, title 만 실패")
    void validationTitleTooLong() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "가".repeat(21) + "\",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").value("제목은 20자 이하여야 합니다."));
    }

    @Test
    @DisplayName("시드 6번 제목(이모지 포함, UTF-16 21자)은 통과")
    void validationEmojiTitlePasses() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"탈모에 좋은 제품 공유해주세요 🙏🏻\",\"content\":\"본문\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("깨진 JSON → 400 INVALID_INPUT, errors 없음")
    void malformedJson() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("쿼리 파라미터 타입 불일치 → 400 INVALID_QUERY")
    void queryTypeMismatch() throws Exception {
        mvc.perform(get("/probe/query").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
    }

    @Test
    @DisplayName("예상치 못한 런타임 예외 → 500 INTERNAL_ERROR, 원인 비노출")
    void unexpected() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }
}
