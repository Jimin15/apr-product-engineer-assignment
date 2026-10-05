package com.apr.community.common.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;

/** backend spec B-01 */
class RequesterIdResolverTest {

    @Test
    @DisplayName("헤더만 있으면 헤더 값")
    void headerOnly() {
        assertThat(RequesterIdResolver.resolve("Jelin", null)).isEqualTo("Jelin");
    }

    @Test
    @DisplayName("쿠키만 있으면 쿠키 값")
    void cookieOnly() {
        assertThat(RequesterIdResolver.resolve(null, "Roidl_08")).isEqualTo("Roidl_08");
    }

    @Test
    @DisplayName("둘 다 같으면 그 값")
    void bothSame() {
        assertThat(RequesterIdResolver.resolve("cos_holic", "cos_holic")).isEqualTo("cos_holic");
    }

    @Test
    @DisplayName("둘 다 유효한데 다르면 USER_ID_MISMATCH")
    void bothDifferent() {
        assertThatThrownBy(() -> RequesterIdResolver.resolve("Jelin", "Roidl_08"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.USER_ID_MISMATCH);
    }

    @Test
    @DisplayName("대소문자가 다르면 다른 값으로 본다")
    void caseSensitive() {
        assertThatThrownBy(() -> RequesterIdResolver.resolve("jelin", "Jelin"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("앞뒤 공백은 제거한다")
    void trimmed() {
        assertThat(RequesterIdResolver.resolve("  Jelin ", null)).isEqualTo("Jelin");
        assertThat(RequesterIdResolver.resolve(" Jelin", "Jelin  ")).isEqualTo("Jelin");
    }

    @Test
    @DisplayName("공백뿐인 값은 없는 것으로 본다")
    void blankIsAbsent() {
        assertThat(RequesterIdResolver.resolve("   ", "Jelin")).isEqualTo("Jelin");
        assertThat(RequesterIdResolver.resolve("", "")).isEqualTo(RequesterIdResolver.DEFAULT_USER);
    }

    @Test
    @DisplayName("둘 다 없으면 apr_tester")
    void noneGivesDefault() {
        assertThat(RequesterIdResolver.resolve(null, null)).isEqualTo("apr_tester");
    }
}
