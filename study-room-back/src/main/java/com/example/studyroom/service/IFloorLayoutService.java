package com.example.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.entity.FloorLayout;

import java.util.Map;

/**
 * 楼层结构图服务
 */
public interface IFloorLayoutService extends IService<FloorLayout> {

    /**
     * 获取已发布的结构图（用户端渲染用），无则返回 null
     */
    FloorLayout getPublished(Long floorId);

    /**
     * 获取草稿（管理员继续编辑用）：优先返回草稿，无草稿时返回已发布版本，再没有返回 null
     */
    FloorLayout getDraft(Long floorId);

    /**
     * 保存草稿（status=0）
     */
    void saveDraft(Long floorId, String layoutJson, Long operatorId);

    /**
     * 发布结构图（status=1），并同步 seat 表：
     * 1. JSON 中的座位：已有 seatId 的更新坐标/类型/区域/状态；无 seatId 的新增并回填；
     * 2. 数据库中存在但 JSON 中已删除的座位：有未来预约的保留并警告，无预约的逻辑删除。
     *
     * @return 更新后的结构图 + 警告信息（{layout, warnings}）
     */
    Map<String, Object> publish(Long floorId, String layoutJson, Long operatorId);
}
