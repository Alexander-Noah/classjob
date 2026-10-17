/*
 * 用户的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/UserMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.UserEntity;

import java.util.List;

@Mapper
public interface UserMapper {

    UserEntity login(@Param("username") String username);

    UserEntity profile(@Param("username") String username);

    List<UserEntity> listUsers();

    UserEntity getUser(@Param("id") Integer id);

    int insertUser(UserEntity user);

    int updateUser(@Param("id") Integer id, @Param("user") UserEntity user);

    int updateUserStatus(@Param("id") Integer id, @Param("status") Integer status);

    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    int updateUserDepartment(@Param("id") Integer id, @Param("department") String department);

    int updateUserPermissions(@Param("id") Integer id, @Param("permissions") String permissions);

    int countSystemAdmins();

    int countUserBusinessReferences(@Param("id") Integer id);

    void deleteUserRoles(@Param("userId") Integer userId);

    void insertUserRole(@Param("userId") Integer userId, @Param("roleCode") String roleCode);

    int deleteUser(@Param("id") Integer id);
}
