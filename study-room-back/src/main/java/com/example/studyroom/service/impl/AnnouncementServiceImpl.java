// AnnouncementServiceImpl.java
package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.entity.Announcement;
import com.example.studyroom.mapper.AnnouncementMapper;
import com.example.studyroom.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl
        extends ServiceImpl<AnnouncementMapper, Announcement>
        implements IAnnouncementService {

    @Override
    public List<Announcement> getPublishedAnnouncements() {
        return list(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getIsDeleted, 0)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime));
    }

    @Override
    public Announcement getPublishedAnnouncement(Long id) {
        Announcement announcement = getById(id);
        if (announcement == null || !Integer.valueOf(1).equals(announcement.getStatus())) {
            throw new BusinessException("公告不存在");
        }
        return announcement;
    }

    @Override
    public IPage<Announcement> listAll(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsDeleted, 0)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime);
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Announcement::getTitle, keyword.trim())
                    .or()
                    .like(Announcement::getContent, keyword.trim()));
        }
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAnnouncement(Announcement announcement) {
        validate(announcement);
        LocalDateTime now = LocalDateTime.now();
        announcement.setId(null);
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (Integer.valueOf(1).equals(announcement.getStatus()) && announcement.getPublishTime() == null) {
            announcement.setPublishTime(now);
        }
        if (announcement.getPublisher() == null || announcement.getPublisher().isBlank()) {
            announcement.setPublisher("管理员");
        }
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        announcement.setIsDeleted(0);
        save(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(Long id, Announcement announcement) {
        Announcement exist = getById(id);
        if (exist == null) {
            throw new BusinessException("公告不存在");
        }
        validate(announcement);

        exist.setTitle(announcement.getTitle());
        exist.setContent(announcement.getContent());
        if (announcement.getPublisher() != null && !announcement.getPublisher().isBlank()) {
            exist.setPublisher(announcement.getPublisher());
        }
        if (announcement.getStatus() != null) {
            exist.setStatus(announcement.getStatus());
        }
        if (Integer.valueOf(1).equals(exist.getStatus()) && exist.getPublishTime() == null) {
            exist.setPublishTime(LocalDateTime.now());
        }
        exist.setUpdateTime(LocalDateTime.now());
        updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("公告不存在");
        }
        removeById(id);
    }

    private void validate(Announcement announcement) {
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (announcement.getTitle().length() > 200) {
            throw new BusinessException("公告标题不能超过200字");
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            throw new BusinessException("公告内容不能为空");
        }
    }
}
