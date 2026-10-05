package com.apr.community.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** product spec D2 · P-42: 제목은 앞뒤 공백 제거 후 코드포인트 1~20 */
class CodePointLengthValidatorTest {

    private final CodePointLengthValidator validator = titleValidator();

    private static CodePointLengthValidator titleValidator() {
        CodePointLengthValidator v = new CodePointLengthValidator();
        v.initialize(new CodePointLength() {
            @Override public int min() { return 1; }
            @Override public int max() { return 20; }
            @Override public String message() { return ""; }
            @Override public Class<?>[] groups() { return new Class[0]; }
            @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
            @Override public Class<CodePointLength> annotationType() { return CodePointLength.class; }
        });
        return v;
    }

    @Test
    @DisplayName("시드 6번 제목(🙏🏻 포함)은 UTF-16 으로 21자지만 코드포인트로 19자라 통과")
    void seedTitleWithEmojiPasses() {
        String title = "탈모에 좋은 제품 공유해주세요 🙏🏻"; // seed.json posts[id=6].title 과 동일. 🙏🏻 = U+1F64F + U+1F3FB (코드포인트 2개)
        assertThat(title.length()).isEqualTo(21);
        assertThat(CodePointLengthValidator.countCodePoints(title)).isEqualTo(19);
        assertThat(validator.isValid(title, null)).isTrue();
    }

    @Test
    @DisplayName("21 코드포인트는 실패")
    void twentyOneCodePointsFails() {
        assertThat(validator.isValid("가".repeat(21), null)).isFalse();
        assertThat(validator.isValid("가".repeat(20), null)).isTrue();
    }

    @Test
    @DisplayName("앞뒤 공백은 빼고 센다")
    void stripsBeforeCounting() {
        assertThat(validator.isValid("  " + "가".repeat(20) + "  ", null)).isTrue();
        assertThat(validator.isValid("  " + "가".repeat(21) + "  ", null)).isFalse();
    }

    @Test
    @DisplayName("공백뿐이면 0자라 실패 (min=1)")
    void blankFails() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @Test
    @DisplayName("null 은 이 검증기가 판단하지 않는다 (@NotBlank 담당)")
    void nullIsIgnored() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
