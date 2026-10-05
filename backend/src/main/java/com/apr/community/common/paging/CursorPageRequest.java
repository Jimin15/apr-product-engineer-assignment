package com.apr.community.common.paging;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;

/**
 * 목록 쿼리 파라미터 (Integration spec §1.3, backend spec B-20 · B-21).
 * - size: 1~100, 생략 시 20. 범위 밖이면 INVALID_QUERY (보정하지 않는다).
 * - cursorCreatedAt · cursorId: 둘 다 있거나 둘 다 없어야 한다. 하나만 있거나 형식이 틀리면 INVALID_QUERY.
 */
public record CursorPageRequest(int size, Cursor cursor) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static CursorPageRequest of(Integer size, String cursorCreatedAt, Long cursorId) {
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_QUERY, "size는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new ApiException(ErrorCode.INVALID_QUERY, "cursorCreatedAt과 cursorId는 함께 보내야 합니다.");
        }
        if (cursorCreatedAt == null) {
            return new CursorPageRequest(resolvedSize, null);
        }
        if (cursorId < 1) {
            throw new ApiException(ErrorCode.INVALID_QUERY, "cursorId는 양의 정수여야 합니다.");
        }
        try {
            return new CursorPageRequest(resolvedSize, new Cursor(Instant.parse(cursorCreatedAt), cursorId));
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.INVALID_QUERY, "cursorCreatedAt은 ISO-8601 UTC 형식이어야 합니다. 예: 2026-08-30T13:55:54Z");
        }
    }

    public boolean hasCursor() {
        return cursor != null;
    }

    /** 다음 묶음이 있는지 알기 위해 한 건 더 읽는다. */
    public int fetchSize() {
        return size + 1;
    }
}
