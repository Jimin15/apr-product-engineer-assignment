package com.apr.community.common.user;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;

/** @CurrentUser String 파라미터에 요청자 id 를 넣어준다. */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader(RequesterIdResolver.HEADER_NAME);
        String cookie = findCookie(request);
        return RequesterIdResolver.resolve(header, cookie);
    }

    /**
     * dev-user 화면이 encodeURIComponent 로 저장하므로 쿠키 값은 URL 디코딩해서 읽는다.
     * 디코딩할 수 없는 값(예: 잘린 % 이스케이프)은 요청 자체의 문제이므로 500 이 아니라 400 으로 거절한다.
     */
    private static String findCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (RequesterIdResolver.COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    throw new ApiException(ErrorCode.INVALID_INPUT, "x-user-id 쿠키 값을 읽을 수 없습니다.");
                }
            }
        }
        return null;
    }
}
