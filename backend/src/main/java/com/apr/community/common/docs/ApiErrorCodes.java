package com.apr.community.common.docs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.apr.community.common.error.ErrorCode;

/**
 * API 문서 전용: 이 API 가 응답할 수 있는 오류 코드 (Integration spec §1.5).
 * OpenApiConfig 가 ErrorCode 에서 상태 · 코드 · 메시지를 읽어 오류 응답과 예시 JSON 을 만든다 — 예시를 손으로 쓰지 않아 실제 응답과 어긋나지 않는다.
 * 요청자를 쓰는 API 의 USER_ID_MISMATCH, 경로 변수가 있는 API 의 INVALID_INPUT 은 자동으로 붙으므로 적지 않는다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

    ErrorCode[] value();
}
