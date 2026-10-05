package com.apr.community.common.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 오류 응답 본문 { code, message, errors? }.
 * errors 는 입력 검증 오류(INVALID_INPUT)에만 있고, 그 외에는 생략된다 (Integration spec C3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.message(), null);
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, null);
    }

    public static ErrorResponse invalidInput(List<FieldError> errors) {
        return new ErrorResponse(ErrorCode.INVALID_INPUT.name(), ErrorCode.INVALID_INPUT.message(), errors);
    }
}
