package com.example.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studyroom.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    // 根据楼层查询座位（带区域/楼层/当前占用者信息）
    List<Seat> selectSeatsWithUser(@Param("floorId") Long floorId, @Param("areaId") Long areaId, @Param("userId") Long userId);

    // 查询所有座位（带区域/楼层/当前占用者信息，可按区域过滤）
    List<Seat> selectAllSeatsWithUser(@Param("areaId") Long areaId, @Param("userId") Long userId);

    // 行锁查询座位（用于预约时防止并发超卖）
    Seat selectByIdForUpdate(@Param("id") Long id);
}
