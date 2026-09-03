package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 区域（房间）实体：楼层下的自习区域/房间
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("area")
public class Area extends BaseEntity {

    /** 关联楼层ID */
    private Long floorId;

    /** 区域名称 (例如: "A区"、"101自习室") */
    private String areaName;

    /** 区域编号/排序号 */
    private Integer sortOrder;

    /** 区域状态 (0:禁用, 1:启用) */
    private Integer status;

    /** 备注信息 */
    private String remark;
}
