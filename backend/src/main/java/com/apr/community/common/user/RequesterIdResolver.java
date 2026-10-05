package com.apr.community.common.user;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;

/**
 * x-user-id 헤더 · 쿠키 값으로 요청자를 정한다 (backend spec B-01).
 * - 앞뒤 공백을 제거하고, 비었거나 공백뿐이면 "없음"으로 본다.
 * - 한쪽만 유효하면 그 값, 둘 다 없으면 DEFAULT_USER.
 * - 둘 다 유효한데 다르면 USER_ID_MISMATCH. 비교는 대소문자를 구분한다.
 */
public final class RequesterIdResolver {

    public static final String HEADER_NAME = "x-user-id";
    public static final String COOKIE_NAME = "x-user-id";
    public static final String DEFAULT_USER = "apr_tester";

    private RequesterIdResolver() {
    }

    public static String resolve(String headerValue, String cookieValue) {
        String header = normalize(headerValue);
        String cookie = normalize(cookieValue);

        if (header != null && cookie != null && !header.equals(cookie)) {
            throw new ApiException(ErrorCode.USER_ID_MISMATCH);
        }
        if (header != null) {
            return header;
        }
        if (cookie != null) {
            return cookie;
        }
        return DEFAULT_USER;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
