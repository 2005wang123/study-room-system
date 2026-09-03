package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.constant.ReservationStatus;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.entity.Floor;
import com.example.studyroom.entity.FloorLayout;
import com.example.studyroom.entity.Reservation;
import com.example.studyroom.entity.Seat;
import com.example.studyroom.mapper.FloorLayoutMapper;
import com.example.studyroom.mapper.FloorMapper;
import com.example.studyroom.mapper.ReservationMapper;
import com.example.studyroom.mapper.SeatMapper;
import com.example.studyroom.service.IFloorLayoutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloorLayoutServiceImpl extends ServiceImpl<FloorLayoutMapper, FloorLayout> implements IFloorLayoutService {

    private final FloorLayoutMapper floorLayoutMapper;
    private final FloorMapper floorMapper;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final ObjectMapper objectMapper;

    /** 结构图状态：草稿 / 已发布 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;

    @Override
    public FloorLayout getPublished(Long floorId) {
        return getByFloorId(floorId, STATUS_PUBLISHED);
    }

    @Override
    public FloorLayout getDraft(Long floorId) {
        FloorLayout layout = getByFloorId(floorId, STATUS_DRAFT);
        if (layout != null) {
            return layout;
        }
        return getByFloorId(floorId, STATUS_PUBLISHED);
    }

    @Override
    public void saveDraft(Long floorId, String layoutJson, Long operatorId) {
        saveLayout(floorId, layoutJson, operatorId, STATUS_DRAFT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publish(Long floorId, String layoutJson, Long operatorId) {
        // 1. 解析 JSON
        JsonNode root;
        try {
            root = objectMapper.readTree(layoutJson);
        } catch (Exception e) {
            throw new BusinessException("结构图数据格式错误，无法发布");
        }
        if (root == null || !root.isObject()) {
            throw new BusinessException("结构图数据为空，无法发布");
        }

        // 2. 同步座位
        List<String> warnings = new ArrayList<>();
        ArrayNode seatsNode = root.has("seats") && root.get("seats").isArray()
                ? (ArrayNode) root.get("seats") : objectMapper.createArrayNode();

        // 该楼层现有座位（未删除）
        Map<Long, Seat> existingSeats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getFloorId, floorId)
                        .eq(Seat::getIsDeleted, 0))
                .stream()
                .collect(Collectors.toMap(Seat::getId, s -> s, (a, b) -> a));

        Set<Long> keptSeatIds = new HashSet<>();
        for (JsonNode seatNode : seatsNode) {
            if (!seatNode.isObject()) {
                continue;
            }
            Long seatId = seatNode.hasNonNull("seatId") ? seatNode.get("seatId").asLong() : null;
            String seatNo = seatNode.hasNonNull("seatNo") ? seatNode.get("seatNo").asText().trim() : "";
            Integer seatType = seatNode.hasNonNull("seatType") ? seatNode.get("seatType").asInt() : 1;
            Long areaId = seatNode.hasNonNull("areaId") && !seatNode.get("areaId").isNull()
                    ? seatNode.get("areaId").asLong() : null;
            Integer x = seatNode.hasNonNull("x") ? seatNode.get("x").asInt() : null;
            Integer y = seatNode.hasNonNull("y") ? seatNode.get("y").asInt() : null;
            Integer status = seatNode.hasNonNull("status") ? seatNode.get("status").asInt() : 0;

            Seat existing = (seatId != null) ? existingSeats.get(seatId) : null;
            if (existing != null) {
                // 更新已有座位（显式 set，支持清空 areaId）
                checkSeatNoUnique(floorId, seatNo, seatId, existing.getSeatNo());
                LambdaUpdateWrapper<Seat> uw = new LambdaUpdateWrapper<Seat>()
                        .eq(Seat::getId, seatId)
                        .set(Seat::getAreaId, areaId)
                        .set(Seat::getXCoord, x)
                        .set(Seat::getYCoord, y)
                        .set(Seat::getStatus, status);
                if (seatNo != null && !seatNo.isEmpty()) {
                    uw.set(Seat::getSeatNo, seatNo);
                }
                if (seatType != null) {
                    uw.set(Seat::getSeatType, seatType);
                }
                seatMapper.update(null, uw);
                keptSeatIds.add(seatId);
            } else {
                // 新增座位
                if (seatNo == null || seatNo.isEmpty()) {
                    seatNo = generateSeatNo(floorId);
                }
                checkSeatNoUnique(floorId, seatNo, null, null);
                Seat seat = new Seat();
                seat.setFloorId(floorId);
                seat.setSeatNo(seatNo);
                seat.setSeatType(seatType);
                seat.setAreaId(areaId);
                seat.setXCoord(x);
                seat.setYCoord(y);
                seat.setStatus(status);
                seatMapper.insert(seat);
                keptSeatIds.add(seat.getId());
                // 回填 seatId 到 JSON，方便前端下次编辑直接关联
                ((ObjectNode) seatNode).put("seatId", seat.getId());
                ((ObjectNode) seatNode).put("seatNo", seatNo);
            }
        }

        // 3. 处理图中已删除的座位（数据库有、JSON 无）
        Set<Long> deletedIds = new HashSet<>(existingSeats.keySet());
        deletedIds.removeAll(keptSeatIds);
        if (!deletedIds.isEmpty()) {
            // 有未来进行中预约的座位不能删除
            List<Reservation> activeReservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                    .in(Reservation::getSeatId, deletedIds)
                    .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE)
                    .eq(Reservation::getIsDeleted, 0)
                    .gt(Reservation::getEndTime, LocalDateTime.now()));
            Set<Long> protectedIds = activeReservations.stream()
                    .map(Reservation::getSeatId)
                    .collect(Collectors.toSet());
            for (Long id : deletedIds) {
                Seat seat = existingSeats.get(id);
                if (protectedIds.contains(id)) {
                    warnings.add("座位 " + seat.getSeatNo() + " 存在进行中的预约，已保留（如需删除请先取消预约）");
                } else {
                    seatMapper.deleteById(id); // 逻辑删除
                }
            }
        }

        // 4. 保存已发布版本
        String updatedJson;
        try {
            updatedJson = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BusinessException("结构图序列化失败");
        }
        saveLayout(floorId, updatedJson, operatorId, STATUS_PUBLISHED);

        Map<String, Object> result = new HashMap<>();
        result.put("layoutJson", updatedJson);
        result.put("warnings", warnings);
        return result;
    }

    // ============ 私有方法 ============

    private FloorLayout getByFloorId(Long floorId, int status) {
        return floorLayoutMapper.selectOne(new LambdaQueryWrapper<FloorLayout>()
                .eq(FloorLayout::getFloorId, floorId)
                .eq(FloorLayout::getStatus, status)
                .eq(FloorLayout::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private void saveLayout(Long floorId, String layoutJson, Long operatorId, int status) {
        FloorLayout existing = floorLayoutMapper.selectOne(new LambdaQueryWrapper<FloorLayout>()
                .eq(FloorLayout::getFloorId, floorId)
                .eq(FloorLayout::getIsDeleted, 0)
                .last("LIMIT 1"));

        if (existing == null) {
            FloorLayout layout = new FloorLayout();
            layout.setFloorId(floorId);
            layout.setLayoutJson(layoutJson);
            layout.setVersion(1);
            layout.setStatus(status);
            layout.setCreateBy(operatorId);
            layout.setUpdateBy(operatorId);
            floorLayoutMapper.insert(layout);
            return;
        }

        // 乐观锁更新：version 不匹配说明被他人修改过
        int rows = floorLayoutMapper.update(null, new LambdaUpdateWrapper<FloorLayout>()
                .eq(FloorLayout::getId, existing.getId())
                .eq(FloorLayout::getVersion, existing.getVersion())
                .eq(FloorLayout::getIsDeleted, 0)
                .set(FloorLayout::getLayoutJson, layoutJson)
                .set(FloorLayout::getStatus, status)
                .set(FloorLayout::getUpdateBy, operatorId)
                .set(FloorLayout::getVersion, existing.getVersion() + 1)
                .set(FloorLayout::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException("楼层结构图已被他人修改，请刷新后重试");
        }
    }

    /**
     * 校验座位编号在同一楼层内唯一（排除自身）
     */
    private void checkSeatNoUnique(Long floorId, String seatNo, Long selfId, String oldSeatNo) {
        if (seatNo == null || seatNo.isEmpty()) {
            return;
        }
        if (selfId != null && seatNo.equals(oldSeatNo)) {
            return;
        }
        Long count = seatMapper.selectCount(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getFloorId, floorId)
                .eq(Seat::getSeatNo, seatNo)
                .eq(Seat::getIsDeleted, 0)
                .ne(selfId != null, Seat::getId, selfId));
        if (count != null && count > 0) {
            throw new BusinessException("座位编号 " + seatNo + " 在该楼层已存在，请更换编号");
        }
    }

    /**
     * 自动生成座位编号：{楼层号}-{序号}，例如 1-01
     */
    private String generateSeatNo(Long floorId) {
        Floor floor = floorMapper.selectById(floorId);
        String prefix = String.valueOf(floor != null ? floor.getFloorNumber() : floorId);
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getFloorId, floorId)
                .eq(Seat::getIsDeleted, 0));
        Set<String> used = seats.stream().map(Seat::getSeatNo).collect(Collectors.toSet());
        int n = 1;
        while (true) {
            String no = prefix + "-" + String.format("%02d", n);
            if (!used.contains(no)) {
                return no;
            }
            n++;
        }
    }
}
