package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.Floor;

import java.util.List;

public interface IFloorService extends IService<Floor> {
    List<Floor> getAllFloors();

    /** 新增楼层（管理员） */
    void createFloor(Floor floor);

    /** 更新楼层名称/排序等（管理员） */
    void updateFloor(Floor floor);

    /** 删除楼层：仅当楼层下无区域/座位/结构图时允许（软删除） */
    void deleteFloor(Long floorId);
}