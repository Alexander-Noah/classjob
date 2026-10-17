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

    /**
     * 登录的处理逻辑。
     * @param username username参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity login(@Param("username") String username);

    /**
     * profile的处理逻辑。
     * @param username username参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity profile(@Param("username") String username);

    /**
     * 列出用户。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> listUsers();

    /**
     * 获取用户。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity getUser(@Param("id") Integer id);

    /**
     * 插入用户。
     * @param user 用户参数。
     * @return int 类型的处理结果。
     */
    int insertUser(UserEntity user);

    /**
     * 更新用户。
     * @param id 标识参数。
     * @param user 用户参数。
     * @return int 类型的处理结果。
     */
    int updateUser(@Param("id") Integer id, @Param("user") UserEntity user);

    /**
     * 更新用户状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return int 类型的处理结果。
     */
    int updateUserStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 更新密码。
     * @param id 标识参数。
     * @param password 密码参数。
     * @return int 类型的处理结果。
     */
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    /**
     * 更新用户部门。
     * @param id 标识参数。
     * @param department 部门参数。
     * @return int 类型的处理结果。
     */
    int updateUserDepartment(@Param("id") Integer id, @Param("department") String department);

    /**
     * 更新用户权限。
     * @param id 标识参数。
     * @param permissions 权限参数。
     * @return int 类型的处理结果。
     */
    int updateUserPermissions(@Param("id") Integer id, @Param("permissions") String permissions);

    /**
     * 统计SystemAdmins。
     * @return int 类型的处理结果。
     */
    int countSystemAdmins();

    /**
     * 统计用户Business引用。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int countUserBusinessReferences(@Param("id") Integer id);

    /**
     * 删除用户角色。
     * @param userId 用户标识参数。
     */
    void deleteUserRoles(@Param("userId") Integer userId);

    /**
     * 插入用户角色。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     */
    void insertUserRole(@Param("userId") Integer userId, @Param("roleCode") String roleCode);

    /**
     * 删除用户。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteUser(@Param("id") Integer id);
}
