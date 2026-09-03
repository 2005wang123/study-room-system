// AnnouncementCommentLike.java
package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告评论点赞记录实体
 * 说明：点赞记录采用物理删除（取消点赞时删除记录），
 * 不参与逻辑删除，以保证 (comment_id, user_id) 唯一约束在重复点赞场景下可用。
 */
@Data
@TableName("announcement_comment_like")
public class AnnouncementCommentLike {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 评论ID */
    private Long commentId;

    /** 点赞用户ID */
    private Long userId;

    /** 点赞时间 */
    private LocalDateTime createTime;
}
