// AnnouncementComment.java
package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告评论实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement_comment")
public class AnnouncementComment extends BaseEntity {

    /** 所属公告ID */
    private Long announcementId;

    /** 评论用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;
}
