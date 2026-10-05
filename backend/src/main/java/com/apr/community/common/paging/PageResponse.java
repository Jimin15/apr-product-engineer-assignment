package com.apr.community.common.paging;

import java.util.List;
import java.util.function.Function;

/** 목록 응답 { items, total, nextCursor } (Integration spec §1.2). nextCursor 는 더 가져올 항목이 없으면 null 이다. */
public record PageResponse<T>(List<T> items, long total, Cursor nextCursor) {

    /**
     * @param items    이번 묶음 (size 이하)
     * @param hasNext  다음 묶음이 있는지 — 서비스가 size+1 건을 읽어 판단한다
     * @param cursorOf 항목에서 (createdAt, id) 를 꺼내는 함수
     */
    public static <T> PageResponse<T> of(List<T> items, boolean hasNext, long total, Function<T, Cursor> cursorOf) {
        Cursor next = hasNext && !items.isEmpty() ? cursorOf.apply(items.get(items.size() - 1)) : null;
        return new PageResponse<>(List.copyOf(items), total, next);
    }
}
