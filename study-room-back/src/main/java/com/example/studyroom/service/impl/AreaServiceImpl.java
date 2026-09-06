package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.entity.Area;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.mapper.AreaMapper;
import com.example.studyroom.mapper.FloorMapper;
import com.example.studyroom.mapper.SeatMapper;
import com.example.studyroom.service.IAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements IAreaService {

    private final AreaMapper areaMapper;
    private final FloorMapper floorMapper;
    private final SeatMapper seatMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createArea(Area area) {
        checkAreaInput(area, null);
        if (area.getStatus() == null) {
            area.setStatus(1);
        }
        if (area.getSortOrder() == null) {
            area.setSortOrder(0);
        }
        area.setId(null);
        save(area);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArea(Area area) {
        if (area.getId() == null || getById(area.getId()) == null) {
            throw new BusinessException("区域不存在");
        }
        checkAreaInput(area, area.getId());
        updateById(area);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArea(Long areaId) {
        if (areaId == null || getById(areaId) == null) {
            throw new BusinessException("区域不存在");
        }
        // 解绑该区域下所有座位，避免座位指向已删除区域
        seatMapper.update(null, new LambdaUpdateWrapper<Seat>()
                .eq(Seat::getAreaId, areaId)
                .eq(Seat::getIsDeleted, 0)
                .set(Seat::getAreaId, null));
        removeById(areaId);
    }

    private void checkAreaInput(Area area, Long selfId) {
        if (area.getFloorId() == null) {
            throw new BusinessException("请选择所属楼层");
        }
        Floor floor = floorMapper.selectById(area.getFloorId());
        if (floor == null) {
            throw new BusinessException("所属楼层不存在");
        }
        String name = area.getAreaName() == null ? "" : area.getAreaName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("区域名称不能为空");
        }
        area.setAreaName(name);
        LambdaQueryWrapper<Area> wrapper = new LambdaQueryWrapper<Area>()
                .eq(Area::getFloorId, area.getFloorId())
                .eq(Area::getAreaName, name)
                .eq(Area::getIsDeleted, 0);
        if (selfId != null) {
            wrapper.ne(Area::getId, selfId);
        }
        Long count = areaMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该楼层下区域「" + name + "」已存在");
        }
    }
}