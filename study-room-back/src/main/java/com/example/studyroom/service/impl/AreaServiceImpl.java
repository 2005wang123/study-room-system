package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.entity.Area;
import com.example.studyroom.mapper.AreaMapper;
import com.example.studyroom.service.IAreaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements IAreaService {

    @Override
    public List<Area> getAreasByFloor(Long floorId) {
        LambdaQueryWrapper<Area> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Area::getFloorId, floorId)
                .eq(Area::getIsDeleted, 0)
                .eq(Area::getStatus, 1)
                .orderByAsc(Area::getSortOrder)
                .orderByAsc(Area::getId);
        return list(wrapper);
    }
}
