package com.example.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studyroom.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper // 告诉 Spring Boot 这是一个数据访问组件
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 后，自动拥有 insert, deleteById, selectById, updateById 等方法

    /**
     * 查询已被逻辑删除的同名用户（绕过 @TableLogic，用于"重建"被删除用户）
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 1 LIMIT 1")
    User selectDeletedByUsername(@Param("username") String username);

    /**
     * 重建被删除的用户（绕过 @TableLogic，直接更新已删除行并恢复 is_deleted=0）
     */
    @Update("UPDATE sys_user SET username = #{username}, password = #{password}, role = #{role}, "
            + "status = #{status}, id_card = #{idCard}, is_first_login = #{isFirstLogin}, "
            + "password_updated_at = #{passwordUpdatedAt}, create_time = #{createTime}, "
            + "update_time = #{updateTime}, is_deleted = 0 "
            + "WHERE id = #{id} AND is_deleted = 1")
    int rebuildUser(User user);
}