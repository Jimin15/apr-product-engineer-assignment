package com.apr.community.common.error;

import org.springframework.http.HttpStatus;

/** API 오류 코드. 상태 코드 · 코드 문자열 · 기본 메시지는 specs/integration/spec.md §1.5 를 따른다. */
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_QUERY(HttpStatus.BAD_REQUEST, "조회 파라미터가 올바르지 않습니다."),
    USER_ID_MISMATCH(HttpStatus.BAD_REQUEST, "x-user-id 헤더와 쿠키의 값이 서로 다릅니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 글 · 댓글만 수정하거나 삭제할 수 있습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    POST_DELETED(HttpStatus.NOT_FOUND, "삭제된 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    COMMENT_DELETED(HttpStatus.NOT_FOUND, "삭제된 댓글입니다."),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "동시에 들어온 요청과 충돌했습니다. 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
