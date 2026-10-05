package com.apr.community.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 앞뒤 공백을 제거한 문자열의 길이를 유니코드 코드포인트 단위로 검사한다 (product spec D2).
 * 기본 @Size 는 UTF-16 단위라 이모지가 2자로 세어지므로 쓰지 않는다. null 은 검사하지 않는다 (@NotBlank 가 담당).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodePointLengthValidator.class)
public @interface CodePointLength {

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String message() default "길이는 {min}자 이상 {max}자 이하여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
