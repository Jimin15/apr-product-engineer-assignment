package com.apr.community.like;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apr.community.common.user.CurrentUser;
import com.apr.community.like.dto.LikeResponse;

@RestController
@RequestMapping("/api/posts")
public class LikeController {

    private final LikeFacade likeFacade;

    public LikeController(LikeFacade likeFacade) {
        this.likeFacade = likeFacade;
    }

    /** B-15 좋아요 토글 → 200 { isLiked, likeCount } */
    @PostMapping("/{postId}/likes/toggle")
    public LikeResponse toggle(@PathVariable Long postId, @CurrentUser String userId) {
        return likeFacade.toggle(postId, userId);
    }
}
