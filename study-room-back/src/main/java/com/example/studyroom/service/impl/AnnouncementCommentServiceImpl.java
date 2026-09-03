// AnnouncementCommentServiceImpl.java
package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.dto.AnnouncementCommentVO;
import com.example.studyroom.entity.Announcement;
import com.example.studyroom.entity.AnnouncementComment;
import com.example.studyroom.entity.AnnouncementCommentLike;
import com.example.studyroom.entity.User;
import com.example.studyroom.mapper.AnnouncementCommentLikeMapper;
import com.example.studyroom.mapper.AnnouncementCommentMapper;
import com.example.studyroom.mapper.UserMapper;
import com.example.studyroom.service.IAnnouncementCommentService;
import com.example.studyroom.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementCommentServiceImpl
        extends ServiceImpl<AnnouncementCommentMapper, AnnouncementComment>
        implements IAnnouncementCommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final IAnnouncementService announcementService;
    private final UserMapper userMapper;
    private final AnnouncementCommentLikeMapper likeMapper;

    @Override
    public List<AnnouncementCommentVO> listComments(Long announcementId, Long currentUserId) {
        List<AnnouncementComment> comments = list(new LambdaQueryWrapper<AnnouncementComment>()
                .eq(AnnouncementComment::getAnnouncementId, announcementId)
                .eq(AnnouncementComment::getIsDeleted, 0)
                .orderByDesc(AnnouncementComment::getCreateTime));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询评论人昵称
        Set<Long> userIds = comments.stream()
                .map(AnnouncementComment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        // 查询当前用户已点赞的评论ID
        Set<Long> likedIds = Collections.emptySet();
        if (currentUserId != null) {
            likedIds = likeMapper.selectList(new LambdaQueryWrapper<AnnouncementCommentLike>()
                            .eq(AnnouncementCommentLike::getUserId, currentUserId)
                            .in(AnnouncementCommentLike::getCommentId,
                                    comments.stream().map(AnnouncementComment::getId).collect(Collectors.toList())))
                    .stream()
                    .map(AnnouncementCommentLike::getCommentId)
                    .collect(Collectors.toSet());
        }
        Set<Long> finalLikedIds = likedIds;

        return comments.stream().map(c -> {
            AnnouncementCommentVO vo = new AnnouncementCommentVO();
            vo.setId(c.getId());
            vo.setAnnouncementId(c.getAnnouncementId());
            vo.setUserId(c.getUserId());
            vo.setUsername(usernameMap.getOrDefault(c.getUserId(), "用户"));
            vo.setContent(c.getContent());
            vo.setLikeCount(c.getLikeCount() == null ? 0 : c.getLikeCount());
            vo.setCreateTime(c.getCreateTime());
            vo.setLiked(finalLikedIds.contains(c.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementCommentVO addComment(Long announcementId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("评论内容不能超过" + MAX_CONTENT_LENGTH + "字");
        }
        // 仅允许对已发布的公告发表评论
        announcementService.getPublishedAnnouncement(announcementId);

        LocalDateTime now = LocalDateTime.now();
        AnnouncementComment comment = new AnnouncementComment();
        comment.setAnnouncementId(announcementId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        comment.setIsDeleted(0);
        save(comment);

        AnnouncementCommentVO vo = new AnnouncementCommentVO();
        vo.setId(comment.getId());
        vo.setAnnouncementId(announcementId);
        vo.setUserId(userId);
        vo.setUsername(resolveUsername(userId));
        vo.setContent(comment.getContent());
        vo.setLikeCount(0);
        vo.setCreateTime(now);
        vo.setLiked(false);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, Integer userRole) {
        AnnouncementComment comment = getById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        boolean isAdmin = userRole != null && userRole == 1;
        boolean isOwner = comment.getUserId() != null && comment.getUserId().equals(userId);
        if (!isAdmin && !isOwner) {
            throw new BusinessException("无权删除该评论");
        }
        removeById(commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId, Long userId) {
        requireComment(commentId);
        Long existed = likeMapper.selectCount(new LambdaQueryWrapper<AnnouncementCommentLike>()
                .eq(AnnouncementCommentLike::getCommentId, commentId)
                .eq(AnnouncementCommentLike::getUserId, userId));
        if (existed != null && existed > 0) {
            return; // 已点赞，幂等
        }
        AnnouncementCommentLike like = new AnnouncementCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);

        AnnouncementComment comment = getById(commentId);
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        comment.setUpdateTime(LocalDateTime.now());
        updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId, Long userId) {
        requireComment(commentId);
        AnnouncementCommentLike existed = likeMapper.selectOne(new LambdaQueryWrapper<AnnouncementCommentLike>()
                .eq(AnnouncementCommentLike::getCommentId, commentId)
                .eq(AnnouncementCommentLike::getUserId, userId)
                .last("LIMIT 1"));
        if (existed == null) {
            return; // 未点赞，幂等
        }
        likeMapper.deleteById(existed.getId());
        AnnouncementComment comment = getById(commentId);
        int newCount = Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1);
        comment.setLikeCount(newCount);
        comment.setUpdateTime(LocalDateTime.now());
        updateById(comment);
    }

    private AnnouncementComment requireComment(Long commentId) {
        AnnouncementComment comment = getById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }

    private String resolveUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "用户" : user.getUsername();
    }
}
