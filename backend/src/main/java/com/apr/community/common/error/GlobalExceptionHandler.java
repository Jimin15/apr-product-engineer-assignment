package com.apr.community.common.error;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 오류를 { code, message, errors? } 형태로 통일한다.
 * 알 수 없는 경로(404) · 허용되지 않은 메서드(405) 같은 Spring MVC 의 체크 예외는 여기서 잡지 않고 기본 처리에 맡긴다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.code().status())
                .body(ErrorResponse.of(ex.code(), ex.getMessage()));
    }

    /** @Valid 실패 — 잘못된 필드를 모두 모아 errors 로 준다. 순서는 필드 이름 기준으로 고정한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ErrorResponse.invalidInput(errors));
    }

    /** 깨진 JSON · 타입이 맞지 않는 본문 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다."));
    }

    /** 쿼리 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingQuery(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_QUERY.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_QUERY));
    }

    /**
     * 타입 불일치. 같은 예외가 쿼리 파라미터(`?size=abc`)와 경로 변수(`/posts/abc`) 양쪽에서 나오므로 어느 쪽인지 구분해 코드를 정한다.
     * 경로 변수는 "조회 파라미터"가 아니라 요청 입력이므로 INVALID_INPUT 으로 응답한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "경로의 " + ex.getName() + " 형식이 올바르지 않습니다."));
        }
        return ResponseEntity.status(ErrorCode.INVALID_QUERY.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_QUERY));
    }

    /** 예상하지 못한 런타임 오류. 원인은 로그로만 남기고 응답에는 노출하지 않는다. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}
