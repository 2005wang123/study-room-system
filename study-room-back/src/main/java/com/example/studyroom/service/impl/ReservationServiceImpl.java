package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.constant.ReservationStatus;
import com.example.studyroom.common.constant.SeatStatus;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.dto.ReservationVO;
import com.example.studyroom.entity.Area;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.entity.Reservation;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.mapper.AreaMapper;
import com.example.studyroom.mapper.FloorMapper;
import com.example.studyroom.mapper.ReservationMapper;
import com.example.studyroom.mapper.SeatMapper;
import com.example.studyroom.service.IReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationServiceImpl
        extends ServiceImpl<ReservationMapper, Reservation>
        implements IReservationService {

    private final SeatMapper seatMapper;
    private final FloorMapper floorMapper;
    private final AreaMapper areaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceCancelReservation(Long reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        // 只有待签到和使用中的才能强制取消
        if (!List.of(ReservationStatus.PENDING, ReservationStatus.IN_USE).contains(reservation.getStatus())) {
            throw new BusinessException("当前状态不允许强制取消");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        updateById(reservation);

        // 同步释放座位（若无其他进行中预约）
        releaseSeatIfFree(reservation.getSeatId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("当前状态不可签到");
        }
        LocalDateTime now = LocalDateTime.now();
        // 允许提前 30 分钟签到，防止早到无法签到
        if (now.isBefore(reservation.getStartTime().minusMinutes(30))) {
            throw new BusinessException("未到签到时间，请在预约开始前30分钟内签到");
        }
        if (now.isAfter(reservation.getEndTime())) {
            throw new BusinessException("预约已结束，无法签到");
        }
        reservation.setStatus(ReservationStatus.IN_USE);
        updateById(reservation);

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(SeatStatus.IN_USE);
            seatMapper.updateById(seat);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOut(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        if (reservation.getStatus() != ReservationStatus.IN_USE) {
            throw new BusinessException("当前状态不可签退");
        }
        reservation.setStatus(ReservationStatus.COMPLETED);
        updateById(reservation);

        // 释放座位（若无其他进行中预约）
        releaseSeatIfFree(reservation.getSeatId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReservation(Long reservationId, Long userId) {
        Reservation reservation = getOwnedReservation(reservationId, userId);
        // 进行中的预约不允许删除，需先取消/签退
        if (List.of(ReservationStatus.PENDING, ReservationStatus.IN_USE).contains(reservation.getStatus())) {
            throw new BusinessException("进行中的预约不能删除，请先取消或签退");
        }
        // 逻辑删除（@TableLogic 自动改写为 UPDATE is_deleted=1）
        removeById(reservationId);
    }

    @Override
    public List<ReservationVO> getUserReservations(Long userId) {
        List<Reservation> reservations = list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getIsDeleted, 0)
                .orderByDesc(Reservation::getStartTime));

        if (reservations.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询涉及的座位、楼层、区域，避免循环查询
        List<Long> seatIds = reservations.stream()
                .map(Reservation::getSeatId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Seat> seatMap = seatMapper.selectBatchIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));

        List<Long> floorIds = seatMap.values().stream()
                .map(Seat::getFloorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Floor> floorMap = floorIds.isEmpty()
                ? Map.of()
                : floorMapper.selectBatchIds(floorIds).stream()
                        .collect(Collectors.toMap(Floor::getId, f -> f));

        List<Long> areaIds = seatMap.values().stream()
                .map(Seat::getAreaId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Area> areaMap = areaIds.isEmpty()
                ? Map.of()
                : areaMapper.selectBatchIds(areaIds).stream()
                        .collect(Collectors.toMap(Area::getId, a -> a));

        return reservations.stream().map(r -> {
            ReservationVO vo = new ReservationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setSeatId(r.getSeatId());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());

            Seat seat = seatMap.get(r.getSeatId());
            if (seat != null) {
                vo.setSeatNo(seat.getSeatNo());
                vo.setFloorId(seat.getFloorId());
                vo.setAreaId(seat.getAreaId());
                Floor floor = seat.getFloorId() == null ? null : floorMap.get(seat.getFloorId());
                if (floor != null) {
                    vo.setFloorName(floor.getFloorName());
                }
                Area area = seat.getAreaId() == null ? null : areaMap.get(seat.getAreaId());
                if (area != null) {
                    vo.setAreaName(area.getAreaName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<Reservation> getAllReservations(Integer status,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 int pageNum, int pageSize) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Reservation::getStatus, status)
                .ge(startDate != null, Reservation::getStartTime, startDate.atStartOfDay())
                .le(endDate != null, Reservation::getEndTime, endDate.plusDays(1).atStartOfDay())
                .eq(Reservation::getIsDeleted, 0)
                .orderByDesc(Reservation::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        int cleaned = 0;

        // 1. 已过结束时间但仍为"待签到"的预约 → 标记违约
        List<Reservation> expired = baseMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, ReservationStatus.PENDING)
                .lt(Reservation::getEndTime, now)
                .eq(Reservation::getIsDeleted, 0));
        if (!expired.isEmpty()) {
            List<Long> expiredIds = expired.stream().map(Reservation::getId).collect(Collectors.toList());
            baseMapper.update(null, new LambdaUpdateWrapper<Reservation>()
                    .set(Reservation::getStatus, ReservationStatus.VIOLATED)
                    .in(Reservation::getId, expiredIds)
                    .eq(Reservation::getIsDeleted, 0));
            releaseSeats(expired.stream().map(Reservation::getSeatId).distinct().collect(Collectors.toList()));
            cleaned += expired.size();
        }

        // 2. 已过结束时间但仍为"使用中"的预约 → 自动完成并释放座位
        List<Reservation> overdueInUse = baseMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, ReservationStatus.IN_USE)
                .lt(Reservation::getEndTime, now)
                .eq(Reservation::getIsDeleted, 0));
        if (!overdueInUse.isEmpty()) {
            List<Long> ids = overdueInUse.stream().map(Reservation::getId).collect(Collectors.toList());
            baseMapper.update(null, new LambdaUpdateWrapper<Reservation>()
                    .set(Reservation::getStatus, ReservationStatus.COMPLETED)
                    .in(Reservation::getId, ids)
                    .eq(Reservation::getIsDeleted, 0));
            releaseSeats(overdueInUse.stream().map(Reservation::getSeatId).distinct().collect(Collectors.toList()));
            cleaned += overdueInUse.size();
        }

        return cleaned;
    }

    /** 释放一批座位中已无进行中预约的座位 */
    private void releaseSeats(List<Long> seatIds) {
        for (Long seatId : seatIds) {
            releaseSeatIfFree(seatId);
        }
    }

    /** 若座位没有其他进行中的预约，则置为空闲 */
    private void releaseSeatIfFree(Long seatId) {
        if (seatId == null) {
            return;
        }
        long stillActive = baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0));
        if (stillActive == 0) {
            Seat seat = seatMapper.selectById(seatId);
            if (seat != null && seat.getStatus() != null && seat.getStatus() != SeatStatus.FREE) {
                seat.setStatus(SeatStatus.FREE);
                seatMapper.updateById(seat);
            }
        }
    }

    /** 查询本人预约，且做归属校验 */
    private Reservation getOwnedReservation(Long reservationId, Long userId) {
        Reservation reservation = getById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw new BusinessException("预约记录不存在");
        }
        return reservation;
    }
}

