package com.example.studyroom.controller;

import com.example.studyroom.common.Result;
import com.example.studyroom.config.annotation.RequireRole;
import com.example.studyroom.entity.FloorLayout;
import com.example.studyroom.service.IFloorLayoutService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 楼层结构图接口：
 * - 公开读取已发布结构图（用户端渲染）
 * - 管理员保存草稿 / 发布（发布时同步 seat 表）
 */
@RestController
@RequestMapping("/api/study-room/floor-layout")
@RequiredArgsConstructor
public class FloorLayoutController {

    private final IFloorLayoutService floorLayoutService;

    /**
     * 获取已发布的结构图（公开）
     */
    @PermitAll
    @GetMapping("/{floorId}")
    public Result<FloorLayout> getPublished(@PathVariable Long floorId) {
        return Result.success(floorLayoutService.getPublished(floorId));
    }

    /**
     * 获取草稿（管理员，无草稿时返回已发布版本）
     */
    @GetMapping("/{floorId}/draft")
    @RequireRole(role = 1)
    public Result<FloorLayout> getDraft(@PathVariable Long floorId) {
        return Result.success(floorLayoutService.getDraft(floorId));
    }

    /**
     * 保存草稿（管理员）
     */
    @PostMapping("/{floorId}/draft")
    @RequireRole(role = 1)
    public Result<Void> saveDraft(@PathVariable Long floorId,
                                  @RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        floorLayoutService.saveDraft(floorId, body.getOrDefault("layoutJson", ""), userId);
        return Result.success("草稿已保存");
    }

    /**
     * 发布结构图（管理员），并同步 seat 表
     */
    @PostMapping("/{floorId}/publish")
    @RequireRole(role = 1)
    public Result<Map<String, Object>> publish(@PathVariable Long floorId,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = floorLayoutService.publish(floorId, body.getOrDefault("layoutJson", ""), userId);
        return Result.success("发布成功", result);
    }
}
