package com.apr.community.post;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.apr.community.common.paging.CursorPageRequest;
import com.apr.community.common.paging.PageResponse;
import com.apr.community.common.user.CurrentUser;
import com.apr.community.post.dto.PostRequest;
import com.apr.community.post.dto.PostResponse;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** B-10 목록 */
    @GetMapping
    public PageResponse<PostResponse> getPosts(@RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) String cursorCreatedAt,
                                               @RequestParam(required = false) Long cursorId,
                                               @CurrentUser String userId) {
        return postService.getPosts(CursorPageRequest.of(size, cursorCreatedAt, cursorId), userId);
    }

    /** B-11 작성 → 201 + 생성된 게시글 (C2-d) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody PostRequest request, @CurrentUser String userId) {
        return postService.createPost(request, userId);
    }

    /** B-12 상세 */
    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable Long postId, @CurrentUser String userId) {
        return postService.getPost(postId, userId);
    }

    /** B-13 수정 → 수정된 게시글 */
    @PatchMapping("/{postId}")
    public PostResponse updatePost(@PathVariable Long postId, @Valid @RequestBody PostRequest request,
                                   @CurrentUser String userId) {
        return postService.updatePost(postId, request, userId);
    }

    /** B-14 삭제 → 204 (C2-e) */
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long postId, @CurrentUser String userId) {
        postService.deletePost(postId, userId);
    }
}
