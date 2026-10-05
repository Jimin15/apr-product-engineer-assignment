package com.apr.community.common.paging;

import java.time.Instant;

/** 다음 묶음의 시작 기준 = 직전 응답 마지막 항목의 (createdAt, id). 응답의 nextCursor 와 요청의 cursorCreatedAt · cursorId 에 쓴다 (Integration C1). */
public record Cursor(Instant createdAt, Long id) {
}
