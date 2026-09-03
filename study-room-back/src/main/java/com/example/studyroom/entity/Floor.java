package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("floor")
public class Floor extends BaseEntity {

    /** 楼层名称 (例如: "一楼", "二楼") */
    private String floorName;

    /** 楼层底图URL (用于前端绘制地图的背景) */
    private String bgImageUrl;

    /** 楼层状态 (0:禁用, 1:启用) */
    private Integer status;

    /** 楼层编号/排序号 */
    private Integer floorNumber;

    /** 备注信息 */
     private String remark;

    /*
    public Object getStatus() {

        return null;
    }

    public Object getFloorNumber() {
        return null;
    }*/
}