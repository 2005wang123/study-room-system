// AnnouncementCommentController.java
package com.example.studyroom.controller;

import com.example.studyroom.common.Result;
import com.example.studyroom.config.JwtAuthenticationFilter;
import com.example.studyroom.config.annotation.RequirePermission;
import com.example.studyroom.dto.AnnouncementCommentVO;
import com.example.studyroom.service.IAnnouncementCommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公告评论与点赞
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementCommentController {

    private final IAnnouncementCommentService commentService;

    /**
     * 获取公告评论列表（公开可看；携带登录态时返回当前用户是否已点赞）
     */
    @GetMapping("/{announcementId}/comments")
    public Result<List<AnnouncementCommentVO>> listComments(@PathVariable Long announcementId) {
        return Result.success(commentService.listComments(announcementId, getOptionalUserId()));
    }

    /**
     * 发表评论（需登录）
     */
    @PostMapping("/{announcementId}/comments")
    @RequirePermission
    public Result<AnnouncementCommentVO> addComment(@PathVariable Long announcementId,
                                                    @RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        Long userId = currentUserId(request);
        String content = body == null ? null : body.get("content");
        return Result.success(commentService.addComment(announcementId, userId, content));
    }

    /**
     * 删除评论（评论本人或管理员）
     */
    @DeleteMapping("/comments/{commentId}")
    @RequirePermission
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        Integer role = (Integer) request.getAttribute("userRole");
        commentService.deleteComment(commentId, userId, role);
        return Result.success("删除成功");
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    @RequirePermission
    public Result<Void> likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        commentService.likeComment(commentId, currentUserId(request));
        return Result.success("点赞成功");
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/comments/{commentId}/like")
    @RequirePermission
    public Result<Void> unlikeComment(@PathVariable Long commentId, HttpServletRequest request) {
        commentService.unlikeComment(commentId, currentUserId(request));
        return Result.success("已取消点赞");
    }

    /**
     * 从请求属性获取当前登录用户ID（@RequirePermission 拦截后由 AuthInterceptor 注入）
     */
    private Long currentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new com.example.studyroom.common.exception.BusinessException("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 获取可选当前用户ID：公开接口在携带有效 Token 时也能识别用户
     */
    private Long getOptionalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof JwtAuthenticationFilter.JwtPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
