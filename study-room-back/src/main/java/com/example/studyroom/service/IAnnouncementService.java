// IAnnouncementService.java
package com.example.studyroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.Announcement;

import java.util.List;

public interface IAnnouncementService extends IService<Announcement> {

    /** 获取已发布的公告列表（按发布时间倒序） */
    List<Announcement> getPublishedAnnouncements();

    /** 获取已发布的公告详情 */
    Announcement getPublishedAnnouncement(Long id);

    /** 管理员分页查询全部公告（含草稿/下架，支持标题关键字与状态筛选） */
    IPage<Announcement> listAll(int pageNum, int pageSize, String keyword, Integer status);

    /** 新增公告（管理员） */
    void createAnnouncement(Announcement announcement);

    /** 更新公告（管理员） */
    void updateAnnouncement(Long id, Announcement announcement);

    /** 删除公告（管理员，逻辑删除） */
    void deleteAnnouncement(Long id);
}
