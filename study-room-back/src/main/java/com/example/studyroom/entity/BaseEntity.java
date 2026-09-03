package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类 - 所有实体类的父类
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 主键ID (MyBatis-Plus 默认策略：雪花算法)
     */
    @TableId(value = "id", type = IdType.AUTO)  //type = IdType.ASSIGN_ID
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 (0:未删除, 1:已删除)
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}