// Announcement.java
package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement")
public class Announcement extends BaseEntity {

    /** 公告标题 */
    private String title;

    /** 公告内容（正文，支持多段，用换行分隔） */
    private String content;

    /** 发布人 */
    private String publisher;

    /** 状态 0-草稿/下架 1-已发布 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
