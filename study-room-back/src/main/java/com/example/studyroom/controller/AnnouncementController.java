// AnnouncementController.java
package com.example.studyroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.studyroom.common.Result;
import com.example.studyroom.config.annotation.RequireRole;
import com.example.studyroom.entity.Announcement;
import com.example.studyroom.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final IAnnouncementService announcementService;

    /**
     * 获取已发布的公告列表（无需登录）
     */
    @GetMapping
    public Result<List<Announcement>> listPublished() {
        return Result.success(announcementService.getPublishedAnnouncements());
    }

    /**
     * 管理员分页查询全部公告（含草稿/下架，支持标题关键字与状态筛选）
     */
    @GetMapping("/admin/list")
    @RequireRole(role = 1)
    public Result<IPage<Announcement>> listAll(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(announcementService.listAll(pageNum, pageSize, keyword, status));
    }

    /**
     * 获取已发布的公告详情（无需登录）
     */
    @GetMapping("/{id}")
    public Result<Announcement> detail(@PathVariable Long id) {
        return Result.success(announcementService.getPublishedAnnouncement(id));
    }

    /**
     * 新增公告（仅管理员）
     */
    @PostMapping
    @RequireRole(role = 1)
    public Result<Void> create(@RequestBody Announcement announcement) {
        announcementService.createAnnouncement(announcement);
        return Result.success("发布成功");
    }

    /**
     * 更新公告（仅管理员）
     */
    @PutMapping("/{id}")
    @RequireRole(role = 1)
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcementService.updateAnnouncement(id, announcement);
        return Result.success("更新成功");
    }

    /**
     * 删除公告（仅管理员，逻辑删除）
     */
    @DeleteMapping("/{id}")
    @RequireRole(role = 1)
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success("删除成功");
    }
}
