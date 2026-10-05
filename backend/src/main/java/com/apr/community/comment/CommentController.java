package com.apr.community.comment;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.apr.community.comment.dto.CommentRequest;
import com.apr.community.comment.dto.CommentResponse;
import com.apr.community.common.paging.CursorPageRequest;
import com.apr.community.common.paging.PageResponse;
import com.apr.community.common.user.CurrentUser;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** B-16 댓글 목록 — 요청자가 필요 없어 @CurrentUser 를 두지 않는다 (plan B11-a) */
    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> getComments(@PathVariable Long postId,
                                                     @RequestParam(required = false) Integer size,
                                                     @RequestParam(required = false) String cursorCreatedAt,
                                                     @RequestParam(required = false) Long cursorId) {
        return commentService.getComments(postId, CursorPageRequest.of(size, cursorCreatedAt, cursorId));
    }

    /** B-17 댓글 작성 → 201 */
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest request,
                                         @CurrentUser String userId) {
        return commentService.createComment(postId, request, userId);
    }

    /** B-18 댓글 삭제 → 204 */
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, @CurrentUser String userId) {
        commentService.deleteComment(commentId, userId);
    }
}
