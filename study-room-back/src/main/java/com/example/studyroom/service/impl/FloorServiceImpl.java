package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.entity.Area;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.entity.FloorLayout;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.mapper.AreaMapper;
import com.example.studyroom.mapper.FloorLayoutMapper;
import com.example.studyroom.mapper.FloorMapper;
import com.example.studyroom.mapper.SeatMapper;
import com.example.studyroom.service.IFloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FloorServiceImpl extends ServiceImpl<FloorMapper, Floor> implements IFloorService {

    private final FloorMapper floorMapper;
    private final AreaMapper areaMapper;
    private final SeatMapper seatMapper;
    private final FloorLayoutMapper floorLayoutMapper;

    @Override
    public List<Floor> getAllFloors() {
        LambdaQueryWrapper<Floor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Floor::getIsDeleted, 0)
                .eq(Floor::getStatus, 1)
                .orderByAsc(Floor::getFloorNumber)
                .orderByAsc(Floor::getId);
        return floorMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFloor(Floor floor) {
        String name = trimName(floor.getFloorName());
        if (name.isEmpty()) {
            throw new BusinessException("楼层名称不能为空");
        }
        checkNameUnique(name, null);
        if (floor.getFloorNumber() == null) {
            floor.setFloorNumber(nextFloorNumber());
        }
        if (floor.getStatus() == null) {
            floor.setStatus(1);
        }
        floor.setFloorName(name);
        save(floor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFloor(Floor floor) {
        if (floor.getId() == null) {
            throw new BusinessException("楼层ID不能为空");
        }
        Floor existing = getById(floor.getId());
        if (existing == null) {
            throw new BusinessException("楼层不存在");
        }
        String name = trimName(floor.getFloorName());
        if (name.isEmpty()) {
            throw new BusinessException("楼层名称不能为空");
        }
        checkNameUnique(name, floor.getId());
        Floor update = new Floor();
        update.setId(floor.getId());
        update.setFloorName(name);
        update.setFloorNumber(floor.getFloorNumber() != null ? floor.getFloorNumber() : existing.getFloorNumber());
        update.setStatus(floor.getStatus() != null ? floor.getStatus() : existing.getStatus());
        update.setRemark(floor.getRemark());
        update.setBgImageUrl(floor.getBgImageUrl());
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFloor(Long floorId) {
        if (floorId == null || getById(floorId) == null) {
            throw new BusinessException("楼层不存在");
        }
        Long areaCount = areaMapper.selectCount(new LambdaQueryWrapper<Area>()
                .eq(Area::getFloorId, floorId)
                .eq(Area::getIsDeleted, 0));
        Long seatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getFloorId, floorId)
                .eq(Seat::getIsDeleted, 0));
        Long layoutCount = floorLayoutMapper.selectCount(new LambdaQueryWrapper<FloorLayout>()
                .eq(FloorLayout::getFloorId, floorId)
                .eq(FloorLayout::getIsDeleted, 0));
        if ((areaCount != null && areaCount > 0)
                || (seatCount != null && seatCount > 0)
                || (layoutCount != null && layoutCount > 0)) {
            throw new BusinessException("该楼层下仍有区域/座位/结构图，请先清空后再删除");
        }
        removeById(floorId);
    }

    private String trimName(String name) {
        return name == null ? "" : name.trim();
    }

    private void checkNameUnique(String name, Long selfId) {
        LambdaQueryWrapper<Floor> wrapper = new LambdaQueryWrapper<Floor>()
                .eq(Floor::getFloorName, name)
                .eq(Floor::getIsDeleted, 0);
        if (selfId != null) {
            wrapper.ne(Floor::getId, selfId);
        }
        Long count = floorMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("楼层名称「" + name + "」已存在");
        }
    }

    /** 自动生成排序号：当前最大排序号 + 1 */
    private int nextFloorNumber() {
        List<Floor> floors = floorMapper.selectList(new LambdaQueryWrapper<Floor>()
                .eq(Floor::getIsDeleted, 0)
                .orderByDesc(Floor::getFloorNumber));
        int max = 0;
        for (Floor f : floors) {
            if (f.getFloorNumber() != null && f.getFloorNumber() > max) {
                max = f.getFloorNumber();
            }
        }
        return max + 1;
    }
}