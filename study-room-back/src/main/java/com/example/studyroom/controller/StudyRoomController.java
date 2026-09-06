package com.example.studyroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.studyroom.common.Result;
import com.example.studyroom.config.JwtAuthenticationFilter;
import com.example.studyroom.config.annotation.RequirePermission;
import com.example.studyroom.config.annotation.RequireRole;
import com.example.studyroom.dto.ReservationVO;
import com.example.studyroom.dto.SeatBookingRequest;
import com.example.studyroom.dto.SeatReservationVO;
import com.example.studyroom.entity.Area;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.entity.Reservation;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.service.IAreaService;
import com.example.studyroom.service.IFloorService;
import com.example.studyroom.service.IReservationService;
import com.example.studyroom.service.ISeatService;
import com.example.studyroom.service.IUserService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/study-room")
@RequiredArgsConstructor
public class StudyRoomController {

    private final IFloorService floorService;
    private final IAreaService areaService;
    private final ISeatService seatService;
    private final IReservationService reservationService;
    private final IUserService userService;

    // ============ 楼层管理 ============

    /**
     * 获取所有楼层
     */
    @PermitAll
    @GetMapping("/floors")
    public Result<List<Floor>> getAllFloors() {
        return Result.success(floorService.getAllFloors());
    }

    /**
     * 获取指定楼层的区域列表
     */
    @PermitAll
    @GetMapping("/floors/{floorId}/areas")
    public Result<List<Area>> getAreasByFloor(@PathVariable Long floorId) {
        return Result.success(areaService.getAreasByFloor(floorId));
    }

    /**
     * 获取指定楼层的座位列表（可按区域/日期过滤，返回预约时段与占用比例）
     */
    @PermitAll
    @GetMapping("/floors/{floorId}/seats")
    public Result<List<Seat>> getSeatsByFloor(
            @PathVariable Long floorId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(seatService.getSeats(floorId, areaId, date, getCurrentUserId()));
    }

    // ============ 楼层管理（管理员） ============

    /**
     * 新增楼层（仅管理员，自定义名称与排序号）
     */
    @PostMapping("/admin/floors")
    @RequireRole(role = 1)
    public Result<Void> addFloor(@RequestBody Floor floor) {
        floorService.createFloor(floor);
        return Result.success("楼层创建成功");
    }

    /**
     * 更新楼层（仅管理员，可改名 / 调整排序 / 启停用）
     */
    @PutMapping("/admin/floors/{floorId}")
    @RequireRole(role = 1)
    public Result<Void> updateFloor(@PathVariable Long floorId, @RequestBody Floor floor) {
        floor.setId(floorId);
        floorService.updateFloor(floor);
        return Result.success("楼层更新成功");
    }

    /**
     * 删除楼层（仅管理员，需先清空其下区域 / 座位 / 结构图）
     */
    @DeleteMapping("/admin/floors/{floorId}")
    @RequireRole(role = 1)
    public Result<Void> deleteFloor(@PathVariable Long floorId) {
        floorService.deleteFloor(floorId);
        return Result.success("楼层删除成功");
    }

    // ============ 区域管理（管理员） ============

    /**
     * 添加区域（仅管理员）
     */
    @PostMapping("/admin/areas")
    @RequireRole(role = 1)
    public Result<Void> addArea(@RequestBody Area area) {
        areaService.createArea(area);
        return Result.success("区域创建成功");
    }

    /**
     * 更新区域（仅管理员）
     */
    @PutMapping("/admin/areas/{areaId}")
    @RequireRole(role = 1)
    public Result<Void> updateArea(@PathVariable Long areaId, @RequestBody Area area) {
        area.setId(areaId);
        areaService.updateArea(area);
        return Result.success("区域更新成功");
    }

    /**
     * 删除区域（软删除，仅管理员）
     */
    @DeleteMapping("/admin/areas/{areaId}")
    @RequireRole(role = 1)
    public Result<Void> deleteArea(@PathVariable Long areaId) {
        areaService.deleteArea(areaId);
        return Result.success("区域删除成功");
    }

    // ============ 座位管理 ============

    /**
     * 获取座位列表（可按楼层/区域/日期过滤，公开）
     */
    @PermitAll
    @GetMapping("/seats")
    public Result<List<Seat>> getSeats(
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(seatService.getSeats(floorId, areaId, date, getCurrentUserId()));
    }

    /**
     * 获取座位详情（公开）
     */
    @PermitAll
    @GetMapping("/seats/{seatId}")
    public Result<Seat> getSeatDetail(@PathVariable Long seatId) {
        return Result.success(seatService.getSeatById(seatId));
    }

    /**
     * 获取座位在某一天的进行中预约时段（公开，用于前端标记不可预约时段）
     */
    @PermitAll
    @GetMapping("/seats/{seatId}/reservations")
    public Result<List<SeatReservationVO>> getSeatReservations(
            @PathVariable Long seatId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(seatService.getSeatReservations(seatId, date));
    }

    /**
     * 预约座位（需要登录）
     */
    @PostMapping("/seats/book")
    @RequirePermission
    public Result<Void> bookSeat(@Valid @RequestBody SeatBookingRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 检查用户状态
        if (!userService.isUserActive(userId)) {
            return Result.error("账户已被禁用，请联系管理员");
        }
        seatService.bookSeat(request, userId);
        return Result.success("预约成功");
    }

    /**
     * 取消预约（需要登录，可按预约记录ID精确取消）
     */
    @DeleteMapping("/seats/{seatId}/cancel")
    @RequirePermission
    public Result<Void> cancelBooking(@PathVariable Long seatId,
                                      @RequestParam(required = false) Long reservationId,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录");
        }
        seatService.cancelBooking(seatId, reservationId, userId);
        return Result.success("取消预约成功");
    }

    /**
     * 签到（待签到 -> 使用中，需要登录）
     */
    @PostMapping("/check-in")
    @RequirePermission
    public Result<Void> checkIn(@RequestParam Long reservationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.checkIn(reservationId, userId);
        return Result.success("签到成功");
    }

    /**
     * 签退（使用中 -> 已完成，需要登录）
     */
    @PostMapping("/check-out")
    @RequirePermission
    public Result<Void> checkOut(@RequestParam Long reservationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.checkOut(reservationId, userId);
        return Result.success("签退成功");
    }

    /**
     * 删除自己的预约记录（仅已完成/违约/已取消等历史记录，逻辑删除）
     */
    @DeleteMapping("/reservations/{reservationId}")
    @RequirePermission
    public Result<Void> deleteReservation(@PathVariable Long reservationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.deleteReservation(reservationId, userId);
        return Result.success("预约记录已删除");
    }

    // ============ 管理员功能 ============

    /**
     * 添加座位（仅管理员）
     */
    @PostMapping("/admin/seats")
    @RequireRole(role = 1)
    public Result<Void> addSeat(@Valid @RequestBody Seat seat) {
        seatService.addSeat(seat);
        return Result.success("添加座位成功");
    }

    /**
     * 更新座位状态（仅管理员）
     * status: 0-空闲 1-已预约 2-使用中 3-维修中
     */
    @PutMapping("/admin/seats/{seatId}/status")
    @RequireRole(role = 1)
    public Result<Void> updateSeatStatus(
            @PathVariable Long seatId,
            @RequestParam Integer status) {
        seatService.updateSeatStatus(seatId, status);
        return Result.success("更新座位状态成功");
    }

    /**
     * 删除座位（软删除，仅管理员）
     */
    @DeleteMapping("/admin/seats/{seatId}")
    @RequireRole(role = 1)
    public Result<Void> deleteSeat(@PathVariable Long seatId) {
        seatService.deleteSeat(seatId);
        return Result.success("删除座位成功");
    }

    /**
     * 获取所有预约记录（仅管理员，分页 + 筛选）
     */
    @GetMapping("/admin/reservations")
    @RequireRole(role = 1)
    public Result<IPage<Reservation>> getAllReservations(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<Reservation> page = reservationService.getAllReservations(
                status, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取用户自己的预约记录（需要登录）
     */
    @GetMapping("/my-reservations")
    @RequirePermission
    public Result<List<ReservationVO>> getMyReservations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(reservationService.getUserReservations(userId));
    }

    /**
     * 强制取消预约（管理员）
     */
    @DeleteMapping("/admin/reservations/{reservationId}")
    @RequireRole(role = 1)
    public Result<Void> forceCancelReservation(@PathVariable Long reservationId) {
        reservationService.forceCancelReservation(reservationId);
        return Result.success("取消预约成功");
    }

    /**
     * 从 SecurityContext 中获取当前登录用户ID（公开接口也可识别登录用户），未登录返回 null
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}

