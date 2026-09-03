package com.example.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studyroom.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}