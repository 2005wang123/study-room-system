package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.constant.ReservationStatus;
import com.example.studyroom.common.constant.SeatStatus;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.dto.SeatBookingRequest;
import com.example.studyroom.dto.SeatReservationVO;
import com.example.studyroom.entity.Reservation;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.mapper.ReservationMapper;
import com.example.studyroom.mapper.SeatMapper;
import com.example.studyroom.mapper.UserMapper;
import com.example.studyroom.service.ISeatService;
import com.example.studyroom.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements ISeatService {

    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final IUserService userService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 开放时间段 */
    private static final LocalTime OPEN_START = LocalTime.of(8, 0);
    private static final LocalTime OPEN_END = LocalTime.of(21, 30);

    @Override
    public List<Seat> getSeats(Long floorId, Long areaId, LocalDate date, Long userId) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Seat> seats;
        if (floorId != null) {
            seats = baseMapper.selectSeatsWithUser(floorId, areaId, userId);
        } else {
            seats = baseMapper.selectAllSeatsWithUser(areaId, userId);
        }
        if (seats == null || seats.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> seatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
        Map<Long, List<SeatReservationVO>> resMap = loadReservationsBySeatIds(seatIds, targetDate);

        for (Seat seat : seats) {
            List<SeatReservationVO> reservations = resMap.getOrDefault(seat.getId(), new ArrayList<>());
            seat.setReservations(reservations);
            seat.setBookedRatio(computeBookedRatio(reservations, targetDate));
            if (userId != null) {
                seat.setMyReservationId(reservations.stream()
                        .filter(r -> userId.equals(r.getUserId()))
                        .map(SeatReservationVO::getId)
                        .findFirst()
                        .orElse(null));
            }
        }
        return seats;
    }

    @Override
    public List<SeatReservationVO> getSeatReservations(Long seatId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return loadReservationsBySeatIds(Collections.singletonList(seatId), targetDate)
                .getOrDefault(seatId, new ArrayList<>());
    }

    /**
     * 批量加载多个座位在指定日期的进行中预约（待签到/使用中）
     */
    private Map<Long, List<SeatReservationVO>> loadReservationsBySeatIds(List<Long> seatIds, LocalDate date) {
        Map<Long, List<SeatReservationVO>> result = new HashMap<>();
        if (seatIds == null || seatIds.isEmpty()) {
            return result;
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .in(Reservation::getSeatId, seatIds)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0)
                .lt(Reservation::getStartTime, dayEnd)
                .gt(Reservation::getEndTime, dayStart)
                .orderByAsc(Reservation::getStartTime));
        if (reservations.isEmpty()) {
            return result;
        }

        // 批量加载用户名
        Set<Long> userIds = reservations.stream()
                .map(Reservation::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds)
                    .forEach(u -> userNames.put(u.getId(), u.getUsername()));
        }

        for (Reservation r : reservations) {
            SeatReservationVO vo = new SeatReservationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setUserName(userNames.get(r.getUserId()));
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setStatus(r.getStatus());
            result.computeIfAbsent(r.getSeatId(), k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 计算已预约时长占开放时段(08:00-21:30)的比例，返回 0~1
     */
    private double computeBookedRatio(List<SeatReservationVO> reservations, LocalDate date) {
        LocalDateTime openStart = date.atTime(OPEN_START);
        LocalDateTime openEnd = date.atTime(OPEN_END);
        long totalMinutes = Duration.between(openStart, openEnd).toMinutes();
        if (totalMinutes <= 0) {
            return 0;
        }
        long bookedMinutes = 0;
        for (SeatReservationVO r : reservations) {
            LocalDateTime s = r.getStartTime();
            LocalDateTime e = r.getEndTime();
            LocalDateTime is = s.isAfter(openStart) ? s : openStart;
            LocalDateTime ie = e.isBefore(openEnd) ? e : openEnd;
            if (ie.isAfter(is)) {
                bookedMinutes += Duration.between(is, ie).toMinutes();
            }
        }
        return Math.min(1.0, (double) bookedMinutes / totalMinutes);
    }

    @Override
    public Seat getSeatById(Long seatId) {
        Seat seat = getById(seatId); // 自动处理逻辑删除
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }
        return seat;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bookSeat(SeatBookingRequest request, Long userId) {
        // 0. 信用积分校验：积分为0且在24小时禁约期内不允许预约
        userService.checkBookingAllowed(userId);

        Long seatId = request.getSeatId();

        // 1. 解析并校验预约时间段
        LocalDateTime startTime = parseBookingTime(request.getStartTime(), "开始时间");
        LocalDateTime endTime = parseBookingTime(request.getEndTime(), "结束时间");
        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            throw new BusinessException("开始时间不能早于当前时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < 10) {
            throw new BusinessException("预约时长不能少于10分钟");
        }
        // 同一预约仅限当天开放时段内（08:00-21:30），当天预约可约至闭馆
        if (!endTime.toLocalDate().equals(startTime.toLocalDate())) {
            throw new BusinessException("预约时间必须为同一天");
        }
        if (startTime.toLocalTime().isBefore(OPEN_START)
                || endTime.toLocalTime().isAfter(OPEN_END)) {
            throw new BusinessException("请在开放时间08:00-21:30内预约");
        }

        // 2. 行锁锁定座位，防止并发抢座（同一座位同一时刻只有一个事务能通过）
        Seat seat = seatMapper.selectByIdForUpdate(seatId);
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }
        if (seat.getStatus() != null && seat.getStatus() == SeatStatus.MAINTENANCE) {
            throw new BusinessException("该座位正在维修中，暂时不可预约");
        }

        // 3. 检查用户是否已有进行中的预约
        long activeCount = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0));
        if (activeCount > 0) {
            throw new BusinessException("您已有进行中的预约，请先签到或取消后再预约");
        }

        // 4. 校验该座位在目标时间段是否已被预约（时间段冲突检测）
        long hasConflict = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0)
                .and(w -> w
                        .between(Reservation::getStartTime, startTime, endTime)
                        .or().between(Reservation::getEndTime, startTime, endTime)
                        .or().apply("{0} BETWEEN start_time AND end_time", startTime)
                        .or().apply("{0} BETWEEN start_time AND end_time", endTime)
                ));
        if (hasConflict > 0) {
            throw new BusinessException("该时段已被预约，请选择其他时间或座位");
        }

        // 5. 创建预约记录
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setSeatId(seatId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.PENDING);
        reservationMapper.insert(reservation);

        // 说明：不再把座位整体置为“已预约”。
        // 座位支持一天内多个时间段分别预约，颜色由前端根据当日已预约时长比例展示。
    }

    /**
     * 解析预约时间，格式 yyyy-MM-dd HH:mm（兼容 ISO 格式）
     */
    private LocalDateTime parseBookingTime(String time, String field) {
        if (time == null || time.isBlank()) {
            throw new BusinessException(field + "不能为空");
        }
        String trimmed = time.trim();
        try {
            return LocalDateTime.parse(trimmed, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e2) {
                throw new BusinessException(field + "格式不正确，应为 yyyy-MM-dd HH:mm");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long seatId, Long reservationId, Long userId) {
        // 1. 查询进行中的预约记录（可指定 reservationId，不指定则取最早一条）
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0);
        if (reservationId != null) {
            wrapper.eq(Reservation::getId, reservationId);
        }
        wrapper.orderByAsc(Reservation::getStartTime).orderByAsc(Reservation::getId);
        List<Reservation> reservations = reservationMapper.selectList(wrapper);
        if (reservations == null || reservations.isEmpty()) {
            throw new BusinessException("未找到该预约记录");
        }
        Reservation reservation = reservations.get(0);

        // 2. 更新预约状态为已取消
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationMapper.updateById(reservation);

        // 3. 释放座位（若无其他进行中预约）
        releaseSeatIfFree(seatId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSeat(Seat seat) {
        save(seat);
    }

    @Override
    public void updateSeatStatus(Long seatId, Integer status) {
        if (status == null || status < SeatStatus.FREE || status > SeatStatus.MAINTENANCE) {
            throw new BusinessException("状态值不合法，允许值: 0-空闲 1-已预约 2-使用中 3-维修中");
        }
        Seat seat = getSeatById(seatId);
        seat.setStatus(status);
        updateById(seat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeat(Long seatId) {
        // 直接使用逻辑删除，无需手动 set isDeleted
        removeById(seatId);
    }

    /**
     * 若座位没有其他进行中的预约，则置为空闲；否则保持不变
     */
    private void releaseSeatIfFree(Long seatId) {
        if (seatId == null) {
            return;
        }
        long activeCount = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                .eq(Reservation::getIsDeleted, 0));
        if (activeCount == 0) {
            Seat seat = seatMapper.selectById(seatId);
            if (seat != null && seat.getStatus() != null && seat.getStatus() != SeatStatus.FREE) {
                seat.setStatus(SeatStatus.FREE);
                seatMapper.updateById(seat);
            }
        }
    }
}
