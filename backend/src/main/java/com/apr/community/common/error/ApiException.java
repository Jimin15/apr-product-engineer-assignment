package com.apr.community.common.error;

/** 비즈니스 규칙 위반을 ErrorCode 로 표현하는 예외. GlobalExceptionHandler 가 응답으로 바꾼다. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.message());
        this.code = code;
    }

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
