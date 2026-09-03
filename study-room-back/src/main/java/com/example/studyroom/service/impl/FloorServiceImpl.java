// FloorServiceImpl.java
package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.mapper.FloorMapper;
import com.example.studyroom.service.IFloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FloorServiceImpl extends ServiceImpl<FloorMapper, Floor> implements IFloorService {

    private final FloorMapper floorMapper;

    @Override
    public List<Floor> getAllFloors() {
        LambdaQueryWrapper<Floor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Floor::getIsDeleted, 0)
                .eq(Floor::getStatus, 1)
                .orderByAsc(Floor::getFloorNumber);
        return floorMapper.selectList(wrapper);
    }

    /**
     * 实现 IService 中定义的批量保存方法
     * @param collection 待保存的实体集合
     * @param batchSize  每批次插入的数量
     * @return 是否保存成功
     */
    @Override
    public boolean saveBatch(Collection<Floor> collection, int batchSize) {
        // 方式1：如果 FloorMapper 继承了 BaseMapper 且项目中配置了 MyBatis-Plus 的批量插入插件
        // return floorMapper.insertBatchSomeColumn(collection) > 0;

        // 方式2：通用分批手动插入（无需额外插件）
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        int count = 0;
        List<Floor> list = new java.util.ArrayList<>(collection);
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<Floor> batch = list.subList(i, end);
            for (Floor floor : batch) {
                count += floorMapper.insert(floor);
            }
        }
        return count == collection.size();
    }
}