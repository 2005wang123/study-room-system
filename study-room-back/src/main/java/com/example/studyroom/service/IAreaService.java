package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.Area;

import java.util.List;

public interface IAreaService extends IService<Area> {

    /** 获取指定楼层的启用区域列表 */
    List<Area> getAreasByFloor(Long floorId);
}
