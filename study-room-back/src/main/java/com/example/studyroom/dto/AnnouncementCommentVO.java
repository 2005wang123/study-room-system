// AnnouncementCommentVO.java
package com.example.studyroom.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告评论视图对象（含评论人信息与当前用户点赞状态）
 */
@Data
public class AnnouncementCommentVO {

    private Long id;
    private Long announcementId;
    private Long userId;
    /** 评论人用户名/学号 */
    private String username;
    private String content;
    private Integer likeCount;
    private LocalDateTime createTime;
    /** 当前登录用户是否已点赞 */
    private Boolean liked;
}
