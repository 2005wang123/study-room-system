package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.dto.SeatBookingRequest;
import com.example.studyroom.dto.SeatReservationVO;
import com.example.studyroom.entity.Seat;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public interface ISeatService extends IService<Seat> {

    void cancelBooking(Long seatId, Long reservationId, Long userId);

    void deleteSeat(Long seatId);

    void updateSeatStatus(Long seatId, Integer status);

    void addSeat(@Valid Seat seat);

    /**
     * 查询座位列表（可按楼层/区域过滤），并附带指定日期的预约时段、已预约比例等信息
     *
     * @param floorId 楼层ID，可为空表示全部楼层
     * @param areaId  区域ID，可为空表示全部区域
     * @param date    查看日期，为空默认今天
     * @param userId  当前登录用户ID，可为空（游客）
     */
    List<Seat> getSeats(Long floorId, Long areaId, LocalDate date, Long userId);

    Seat getSeatById(Long seatId);

    void bookSeat(SeatBookingRequest request, Long userId);

    /**
     * 查询指定座位在某一天的进行中预约时段（用于前端标记不可预约时段）
     */
    List<SeatReservationVO> getSeatReservations(Long seatId, LocalDate date);
}
