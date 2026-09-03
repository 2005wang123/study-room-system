//IFloor.java
package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.Floor;

import java.util.List;

public interface IFloorService extends IService<Floor> {
    List<Floor> getAllFloors();
}