package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.Area;

import java.util.List;

public interface IAreaService extends IService<Area> {

    /** 获取指定楼层的启用区域列表 */
    List<Area> getAreasByFloor(Long floorId);

    /** 新增区域（管理员，带名称/楼层校验） */
    void createArea(Area area);

    /** 更新区域（管理员，带名称/楼层校验） */
    void updateArea(Area area);

    /** 删除区域（软删除，并解绑其下座位） */
    void deleteArea(Long areaId);
}