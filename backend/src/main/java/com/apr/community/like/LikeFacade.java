package com.apr.community.like;

import java.util.concurrent.ThreadLocalRandom;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.like.dto.LikeResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 토글의 재시도 (plan B8-b · B8-c).
 * - LikeService.toggle 은 별도 빈이므로 매 시도가 프록시를 거쳐 새 트랜잭션으로 열린다. 이 클래스에는 @Transactional 을 두지 않는다.
 * - 최초 시도를 포함해 최대 3회. 재시도 대상은 낙관적 락 충돌과 post_likes 유니크 제약 위반뿐이다.
 *   같은 DataIntegrityViolationException 이라도 CHECK(like_count >= 0) 위반은 버그이므로 재시도하지 않고 그대로 올린다 (→ 500).
 * - 재시도 전에 지수 백오프 + jitter 로 대기한다 (1회차 10~20ms, 2회차 20~40ms). 같은 순간에 들어온 요청들이 재시도에서 다시 겹치는 것을 줄인다.
 *   단, 겹친 묶음에서는 한 번에 하나만 커밋되므로 한 묶음에서 성공할 수 있는 수의 상한은 시도 횟수가 정한다 (plan B8 측정 기록).
 * - 3회 모두 충돌하면 409 CONCURRENT_UPDATE.
 */
@Slf4j
@Component
public class LikeFacade {

    static final int MAX_ATTEMPTS = 3;
    static final String POST_LIKES_UNIQUE_CONSTRAINT = "uk_post_likes_post_user";
    /** n번째 재시도 전 대기의 기준값. 실제 대기는 base·2^(n-1) 과 그 2배 사이의 무작위 값 (지수 백오프 + jitter) */
    static final long BACKOFF_BASE_MS = 10;

    private final LikeService likeService;
    private final Backoff backoff;

    @Autowired
    public LikeFacade(LikeService likeService) {
        this(likeService, LikeFacade::randomSleep);
    }

    LikeFacade(LikeService likeService, Backoff backoff) {
        this.likeService = likeService;
        this.backoff = backoff;
    }

    public LikeResponse toggle(Long postId, String userId) {
        for (int attempt = 1; ; attempt++) {
            try {
                // 1. 토글 시도 — LikeService 를 부를 때마다 새 트랜잭션이 열린다
                return likeService.toggle(postId, userId);
            } catch (OptimisticLockingFailureException e) {
                // 2-1. 다른 요청이 먼저 카운터를 바꿈 (version 충돌) → 재시도
                failOrRetry(attempt, postId, userId, "optimistic lock");
            } catch (DataIntegrityViolationException e) {
                // 2-2. 같은 사용자가 동시에 좋아요 → post_likes 유니크 위반만 재시도, 그 밖의 제약 위반(CHECK 등)은 버그라 그대로 올린다
                if (!isPostLikesUniqueViolation(e)) {
                    throw e;
                }
                failOrRetry(attempt, postId, userId, "duplicate like");
            }
        }
    }

    private void failOrRetry(int attempt, Long postId, String userId, String cause) {
        // 1. 최대 횟수를 다 썼으면 409
        if (attempt >= MAX_ATTEMPTS) {
            log.warn("Like toggle gave up after {} attempts (post={}, user={}, cause={})", attempt, postId, userId, cause);
            throw new ApiException(ErrorCode.CONCURRENT_UPDATE);
        }
        // 2. 아니면 잠깐 기다렸다가 다시 시도 (지수 백오프 + jitter)
        log.debug("Like toggle conflict, retrying (attempt {}/{}, post={}, user={}, cause={})",
                attempt, MAX_ATTEMPTS, postId, userId, cause);
        backoff.await(attempt);
    }

    /** 재시도 전 대기. 테스트에서는 대기하지 않는 구현으로 바꿔 끼운다. */
    interface Backoff {
        void await(int attempt);
    }

    private static void randomSleep(int attempt) {
        long base = BACKOFF_BASE_MS << (attempt - 1);
        long millis = ThreadLocalRandom.current().nextLong(base, base * 2 + 1);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * post_likes 유니크 제약 위반인지 판별한다.
     * Hibernate 가 추출한 제약 이름(ConstraintViolationException.getConstraintName)을 우선 쓴다 — DB 오류 메시지 형식에 의존하지 않는다.
     * 제약 이름을 얻지 못한 경우(드라이버가 안 주거나 Hibernate 를 거치지 않은 예외)에만 메시지 문자열로 보조 판별한다.
     */
    static boolean isPostLikesUniqueViolation(DataIntegrityViolationException e) {
        // 1. 원인 체인에서 Hibernate 가 추출한 제약 이름을 찾아 비교
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return POST_LIKES_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName().replace("\"", ""));
            }
        }
        // 2. 제약 이름을 못 얻은 경우에만 DB 오류 메시지로 보조 판별
        String message = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        return message != null && message.contains(POST_LIKES_UNIQUE_CONSTRAINT);
    }
}
