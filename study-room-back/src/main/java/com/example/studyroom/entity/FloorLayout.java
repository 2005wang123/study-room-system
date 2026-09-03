package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼层结构图实体：存储管理员绘制的每层楼布局（区域/墙体/座位/文字等）。
 * 每层楼一行（floor_id 唯一），status 区分草稿与已发布版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("floor_layout")
public class FloorLayout extends BaseEntity {

    /** 关联楼层ID */
    private Long floorId;

    /** 楼层结构图 JSON（含区域、墙体、座位、文字、底图等） */
    private String layoutJson;

    /** 版本号（乐观锁，防止多人并发覆盖） */
    private Integer version;

    /** 状态: 0-草稿 1-已发布 */
    private Integer status;

    /** 创建人ID */
    private Long createBy;

    /** 最后更新人ID */
    private Long updateBy;
}
