// IAnnouncementCommentService.java
package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.dto.AnnouncementCommentVO;
import com.example.studyroom.entity.AnnouncementComment;

import java.util.List;

public interface IAnnouncementCommentService extends IService<AnnouncementComment> {

    /**
     * 获取某公告的评论列表（按创建时间倒序）
     *
     * @param announcementId 公告ID
     * @param currentUserId  当前登录用户ID（未登录传 null）
     */
    List<AnnouncementCommentVO> listComments(Long announcementId, Long currentUserId);

    /**
     * 发表评论（普通用户）
     */
    AnnouncementCommentVO addComment(Long announcementId, Long userId, String content);

    /**
     * 删除评论（评论本人或管理员）
     */
    void deleteComment(Long commentId, Long userId, Integer userRole);

    /**
     * 点赞评论（重复点赞幂等，不报错）
     */
    void likeComment(Long commentId, Long userId);

    /**
     * 取消点赞评论（未点赞时幂等，不报错）
     */
    void unlikeComment(Long commentId, Long userId);
}
