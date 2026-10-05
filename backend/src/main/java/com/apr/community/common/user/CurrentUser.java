package com.apr.community.common.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드의 String 파라미터에 붙이면 요청자 id 가 주입된다.
 * 식별 규칙은 RequesterIdResolver 를 따르며, 이 애너테이션을 선언한 API 에서만 헤더 · 쿠키 불일치를 검사한다 (plan B11-a).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
