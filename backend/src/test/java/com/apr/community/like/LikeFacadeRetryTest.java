package com.apr.community.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.like.dto.LikeResponse;

/** plan B8-c: 최초 포함 최대 3회 시도, 재시도 대상은 낙관적 락 충돌 · post_likes 유니크 위반만 */
class LikeFacadeRetryTest {

    private final LikeService likeService = mock(LikeService.class);
    private final java.util.List<Integer> backoffCalls = new java.util.ArrayList<>();
    private final LikeFacade facade = new LikeFacade(likeService, backoffCalls::add);   // 대기 없이 호출만 기록

    private static final LikeResponse OK = new LikeResponse(true, 1);
    private static final ObjectOptimisticLockingFailureException LOCK =
            new ObjectOptimisticLockingFailureException("PostLikeCount", 1L);
    private static final DataIntegrityViolationException DUPLICATE_LIKE =
            new DataIntegrityViolationException("dup", new SQLException(
                    "ERROR: duplicate key value violates unique constraint \"uk_post_likes_post_user\""));
    private static final DataIntegrityViolationException NEGATIVE_COUNT =
            new DataIntegrityViolationException("check", new SQLException(
                    "ERROR: new row for relation \"post_like_counts\" violates check constraint \"ck_post_like_counts_non_negative\""));
    /** 실제 런타임 형태: Hibernate 가 제약 이름을 추출해 둔 예외. 메시지에는 제약 이름이 없다 — 이름 필드로만 판별돼야 한다. */
    private static final DataIntegrityViolationException DUPLICATE_LIKE_BY_NAME =
            new DataIntegrityViolationException("could not execute statement",
                    new ConstraintViolationException("could not execute statement", new SQLException("ERROR"), "uk_post_likes_post_user"));
    private static final DataIntegrityViolationException NEGATIVE_COUNT_BY_NAME =
            new DataIntegrityViolationException("could not execute statement",
                    new ConstraintViolationException("could not execute statement", new SQLException("ERROR"), "ck_post_like_counts_non_negative"));

    @Test
    @DisplayName("Hibernate 가 추출한 제약 이름으로 판별: 유니크 위반은 재시도, CHECK 위반은 전파 (메시지에 이름 없음)")
    void classifiesByConstraintName() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(DUPLICATE_LIKE_BY_NAME).thenReturn(OK);
        assertThat(facade.toggle(1L, "u")).isEqualTo(OK);
        verify(likeService, times(2)).toggle(1L, "u");

        when(likeService.toggle(anyLong(), anyString())).thenThrow(NEGATIVE_COUNT_BY_NAME);
        assertThatThrownBy(() -> facade.toggle(2L, "u")).isSameAs(NEGATIVE_COUNT_BY_NAME);
    }

    @Test
    @DisplayName("낙관적 락 충돌 2회 후 성공 → 총 3회 시도, 결과 반환")
    void retriesOnOptimisticLockAndSucceeds() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(LOCK, LOCK).thenReturn(OK);

        assertThat(facade.toggle(1L, "u")).isEqualTo(OK);
        verify(likeService, times(3)).toggle(1L, "u");
        assertThat(backoffCalls).containsExactly(1, 2);   // 재시도 전마다 대기, 성공 후에는 없음
    }

    @Test
    @DisplayName("3회 모두 충돌 → 409 CONCURRENT_UPDATE, 4번째 시도는 없다")
    void givesUpAfterThreeAttempts() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(LOCK, LOCK, LOCK).thenReturn(OK);

        assertThatThrownBy(() -> facade.toggle(1L, "u"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.CONCURRENT_UPDATE);
        verify(likeService, times(3)).toggle(1L, "u");
        assertThat(backoffCalls).containsExactly(1, 2);   // 포기할 때는 대기하지 않는다
    }

    @Test
    @DisplayName("post_likes 유니크 위반(같은 사용자 연타)도 재시도 대상")
    void retriesOnDuplicateLike() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(DUPLICATE_LIKE).thenReturn(OK);

        assertThat(facade.toggle(1L, "u")).isEqualTo(OK);
        verify(likeService, times(2)).toggle(1L, "u");
    }

    @Test
    @DisplayName("CHECK(like_count >= 0) 위반은 버그이므로 재시도 없이 그대로 올린다 (→ 500)")
    void doesNotRetryOnCheckViolation() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(NEGATIVE_COUNT).thenReturn(OK);

        assertThatThrownBy(() -> facade.toggle(1L, "u")).isSameAs(NEGATIVE_COUNT);
        verify(likeService, times(1)).toggle(1L, "u");
    }

    @Test
    @DisplayName("비즈니스 오류(POST_DELETED)는 재시도하지 않는다")
    void doesNotRetryApiException() {
        when(likeService.toggle(anyLong(), anyString())).thenThrow(new ApiException(ErrorCode.POST_DELETED));

        assertThatThrownBy(() -> facade.toggle(1L, "u"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.POST_DELETED);
        verify(likeService, times(1)).toggle(1L, "u");
    }
}
