package com.apr.community.like;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apr.community.like.dto.LikeResponse;
import com.apr.community.post.PostLikeCount;
import com.apr.community.post.PostLikeCountRepository;
import com.apr.community.post.PostService;

/**
 * 좋아요 토글 한 번 = 트랜잭션 하나 (plan B8).
 * 게시글 확인 → 카운터 조회(version 포함) → 기록 존재 확인 → 추가/삭제 → 카운터 ±1 → 커밋 시 version 검사.
 * 충돌(낙관적 락 · post_likes 유니크 위반)은 이 메서드 밖으로 전파되고, LikeFacade 가 새 트랜잭션으로 재시도한다.
 */
@Service
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostLikeCountRepository postLikeCountRepository;
    private final PostService postService;

    public LikeService(PostLikeRepository postLikeRepository,
                       PostLikeCountRepository postLikeCountRepository,
                       PostService postService) {
        this.postLikeRepository = postLikeRepository;
        this.postLikeCountRepository = postLikeCountRepository;
        this.postService = postService;
    }

    @Transactional
    public LikeResponse toggle(Long postId, String userId) {
        // 1. 게시글이 살아 있는지 확인 (없거나 삭제됐으면 404)
        postService.getActivePost(postId);
        // 2. 좋아요 카운터 조회 (version 포함 — 커밋 시 낙관적 락 검사에 쓰인다)
        PostLikeCount counter = postLikeCountRepository.findById(postId)
                .orElseThrow(() -> new IllegalStateException("like counter missing for post " + postId));

        // 3. 요청자의 좋아요 기록이 있으면 취소, 없으면 추가
        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            // 3-1. 취소: 기록 삭제 + 카운터 −1
            postLikeRepository.delete(existing.get());
            counter.decrease();
            liked = false;
        } else {
            // 3-2. 좋아요: 기록 추가 + 카운터 +1
            postLikeRepository.save(PostLike.of(postId, userId));   // IDENTITY 라 즉시 INSERT → 중복이면 여기서 유니크 위반
            counter.increase();
            liked = true;
        }
        // 4. 처리 후 상태를 응답 (카운터 UPDATE 와 version 검사는 커밋 때 일어난다)
        return new LikeResponse(liked, counter.getLikeCount());
    }
}
