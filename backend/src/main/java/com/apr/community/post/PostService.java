package com.apr.community.post;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.common.paging.Cursor;
import com.apr.community.common.paging.CursorPageRequest;
import com.apr.community.common.paging.PageResponse;
import com.apr.community.post.dto.PostRequest;
import com.apr.community.post.dto.PostResponse;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeCountRepository postLikeCountRepository;
    private final Clock clock;

    public PostService(PostRepository postRepository, PostLikeCountRepository postLikeCountRepository, Clock clock) {
        this.postRepository = postRepository;
        this.postLikeCountRepository = postLikeCountRepository;
        this.clock = clock;
    }

    /**
     * 살아 있는 게시글만 돌려준다. 없으면 POST_NOT_FOUND, 삭제됐으면 POST_DELETED (B-33 · B-34).
     * 댓글 · 좋아요도 이 규칙을 그대로 쓴다 (plan B10-b).
     */
    public Post getActivePost(Long postId) {
        // 1. 게시글이 있는지 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        // 2. 삭제된 게시글인지 확인
        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_DELETED);
        }
        return post;
    }

    /** B-12 상세 */
    public PostResponse getPost(Long postId, String userId) {
        // 1. 살아 있는 게시글 조회 (없거나 삭제됐으면 404)
        Post post = getActivePost(postId);
        // 2. 좋아요 수 · 댓글 수 · 요청자의 좋아요 여부를 붙여 응답
        return toResponses(List.of(post), userId).get(0);
    }

    /** B-10 목록: 최신순, 커서로 이어서 조회 (plan B9) */
    public PageResponse<PostResponse> getPosts(CursorPageRequest request, String userId) {
        // 1. 다음 묶음이 있는지 알기 위해 size + 1 건을 읽는다 (커서가 있으면 그 뒤부터)
        Limit limit = Limit.of(request.fetchSize());
        List<Post> fetched = request.hasCursor()
                ? postRepository.findPageAfter(request.cursor().createdAt(), request.cursor().id(), limit)
                : postRepository.findFirstPage(limit);
        // 2. 삭제를 뺀 전체 개수 (이번 응답 개수가 아님)
        long total = postRepository.countByDeletedAtIsNull();

        // 3. 한 건이 더 읽혔으면 다음 묶음이 있다 → 응답에는 size 건만 담는다
        boolean hasNext = fetched.size() > request.size();
        List<Post> page = hasNext ? fetched.subList(0, request.size()) : fetched;
        // 4. 집계를 붙이고, 마지막 항목의 (createdAt, id)를 nextCursor 로 만든다
        return PageResponse.of(toResponses(page, userId), hasNext, total,
                item -> new Cursor(item.createdAt(), item.id()));
    }

    /** B-11 작성: 제목 trim 저장, 내용 그대로. 좋아요 카운터(0)를 같은 트랜잭션에서 만든다. */
    @Transactional
    public PostResponse createPost(PostRequest request, String userId) {
        // 1. 게시글 저장 (제목은 앞뒤 공백 제거, 작성자는 요청자, 시각은 초 단위)
        Post post = postRepository.save(Post.create(request.normalizedTitle(), request.content(), userId, now()));
        // 2. 좋아요 카운터를 0으로 함께 만든다 (좋아요 토글이 이 행을 갱신한다)
        postLikeCountRepository.save(PostLikeCount.of(post.getId(), 0));
        // 3. 목록 항목과 같은 형태로 응답
        return toResponses(List.of(post), userId).get(0);
    }

    /** B-13 수정: 본인 글만. 존재 · 삭제 확인이 권한 확인보다 먼저다 (C2-c). */
    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request, String userId) {
        // 1. 살아 있는 본인 글인지 확인 (없음 · 삭제 → 404, 남의 글 → 403)
        Post post = getOwnedActivePost(postId, userId);
        // 2. 제목 · 내용 변경 (커밋 시 변경 감지로 UPDATE)
        post.update(request.normalizedTitle(), request.content());
        // 3. 수정된 게시글을 상세와 같은 형태로 응답
        return toResponses(List.of(post), userId).get(0);
    }

    /** B-14 삭제: 게시글의 deleted_at 만 기록한다. 댓글은 건드리지 않는다 (plan B6-c). */
    @Transactional
    public void deletePost(Long postId, String userId) {
        // 1. 살아 있는 본인 글인지 확인 (없음 · 삭제 → 404, 남의 글 → 403)
        Post post = getOwnedActivePost(postId, userId);
        // 2. 삭제 표시만 남긴다 (행은 지우지 않음)
        post.delete(now());
    }

    private Post getOwnedActivePost(Long postId, String userId) {
        // 1. 존재 · 삭제 여부를 먼저 확인
        Post post = getActivePost(postId);
        // 2. 그다음 권한 확인
        if (!post.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return post;
    }

    /** 저장 시각은 초 단위로 자른다 (Integration spec C4). */
    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /** 게시글 묶음에 좋아요 수 · 댓글 수 · 요청자의 좋아요 여부를 붙인다. 묶음당 쿼리 3개로 고정 (N+1 없음). */
    List<PostResponse> toResponses(List<Post> posts, String userId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();

        // 1. 좋아요 수 (게시글 묶음을 한 번에)
        Map<Long, Integer> likeCounts = postRepository.findLikeCounts(ids).stream()
                .collect(Collectors.toMap(PostLikeCountView::getPostId,
                        PostLikeCountView::getLikeCount));
        // 2. 삭제되지 않은 댓글 수 (GROUP BY 한 번)
        Map<Long, Long> commentCounts = postRepository.countActiveComments(ids).stream()
                .collect(Collectors.toMap(PostCommentCount::getPostId,
                        PostCommentCount::getCount));
        // 3. 요청자가 좋아요한 게시글 id
        Set<Long> liked = postRepository.findLikedPostIds(userId, ids);

        // 4. 게시글 순서대로 응답 객체 조립 (댓글이 없는 글은 0)
        return posts.stream()
                .map(post -> PostResponse.of(post,
                        likeCounts.getOrDefault(post.getId(), 0),
                        commentCounts.getOrDefault(post.getId(), 0L),
                        liked.contains(post.getId())))
                .collect(Collectors.toList());
    }
}
