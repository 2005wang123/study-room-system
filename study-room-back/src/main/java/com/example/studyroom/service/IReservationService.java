package com.example.studyroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.dto.ReservationVO;
import com.example.studyroom.entity.Reservation;

import java.time.LocalDate;
import java.util.List;

public interface IReservationService extends IService<Reservation> {

    void forceCancelReservation(Long reservationId);

    /** 签到（待签到 -> 使用中），仅本人可操作 */
    void checkIn(Long reservationId, Long userId);

    /** 签退（使用中 -> 已完成），仅本人可操作 */
    void checkOut(Long reservationId, Long userId);

    /** 删除本人的预约记录（仅已完成/违约/已取消等历史记录，逻辑删除） */
    void deleteReservation(Long reservationId, Long userId);

    /**
     * 获取用户自己的预约记录（带座位、楼层等展示信息）
     */
    List<ReservationVO> getUserReservations(Long userId);

    /** 分页查询所有预约（管理员） */
    IPage<Reservation> getAllReservations(Integer status,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          int pageNum,
                                          int pageSize);

    /** 清理过期预约：待签到超时标记违约、使用中超时自动完成，并释放座位 */
    int cleanExpiredReservations();
}
