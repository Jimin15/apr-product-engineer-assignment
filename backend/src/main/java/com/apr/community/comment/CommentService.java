package com.apr.community.comment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apr.community.comment.dto.CommentRequest;
import com.apr.community.comment.dto.CommentResponse;
import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ErrorCode;
import com.apr.community.common.paging.Cursor;
import com.apr.community.common.paging.CursorPageRequest;
import com.apr.community.common.paging.PageResponse;
import com.apr.community.post.PostService;

/**
 * 댓글은 항상 부모 게시글 상태를 먼저 확인한다 (plan B6-c, C2-c). 게시글 확인 규칙은 PostService 를 재사용한다 (plan B10-b).
 */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final Clock clock;

    public CommentService(CommentRepository commentRepository, PostService postService, Clock clock) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.clock = clock;
    }

    /** B-16 목록: 최신순, 커서로 이어서 조회. 요청자는 필요 없다. */
    public PageResponse<CommentResponse> getComments(Long postId, CursorPageRequest request) {
        // 1. 부모 게시글이 살아 있는지 확인 (없거나 삭제됐으면 404 — 삭제된 글의 댓글도 안 보인다)
        postService.getActivePost(postId);

        // 2. 다음 묶음이 있는지 알기 위해 size + 1 건을 읽는다 (커서가 있으면 그 뒤부터)
        Limit limit = Limit.of(request.fetchSize());
        List<Comment> fetched = request.hasCursor()
                ? commentRepository.findPageAfter(postId, request.cursor().createdAt(), request.cursor().id(), limit)
                : commentRepository.findFirstPage(postId, limit);
        // 3. 삭제를 뺀 전체 댓글 수 (게시글의 commentCount 와 같은 값)
        long total = commentRepository.countByPostIdAndDeletedAtIsNull(postId);

        // 4. 한 건이 더 읽혔으면 다음 묶음이 있다 → 응답에는 size 건만 담고, 마지막 항목으로 nextCursor 를 만든다
        boolean hasNext = fetched.size() > request.size();
        List<Comment> page = hasNext ? fetched.subList(0, request.size()) : fetched;
        return PageResponse.of(page.stream().map(CommentResponse::of).toList(), hasNext, total,
                item -> new Cursor(item.createdAt(), item.id()));
    }

    /** B-17 작성: 내용은 받은 그대로 저장한다. */
    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest request, String userId) {
        // 1. 부모 게시글이 살아 있는지 확인 (없거나 삭제됐으면 404)
        postService.getActivePost(postId);
        // 2. 댓글 저장 (내용 그대로, 작성자는 요청자, 시각은 초 단위)
        Comment comment = commentRepository.save(Comment.create(postId, request.content(), userId, now()));
        return CommentResponse.of(comment);
    }

    /** B-18 삭제. 확인 순서: 댓글 존재 → 부모 게시글 상태 → 댓글 상태 → 권한 (plan B6 구현 시 주의) */
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        // 1. 댓글이 있는지 확인
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
        // 2. 부모 게시글이 살아 있는지 확인 (삭제된 글의 댓글은 POST_DELETED)
        postService.getActivePost(comment.getPostId());
        // 3. 이미 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            throw new ApiException(ErrorCode.COMMENT_DELETED);
        }
        // 4. 본인 댓글인지 확인
        if (!comment.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        // 5. 삭제 표시만 남긴다 (행은 지우지 않음)
        comment.delete(now());
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
