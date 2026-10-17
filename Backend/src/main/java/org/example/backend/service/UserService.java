/*
 * 用户能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.UserEntity;

import java.util.List;

public interface UserService {
    /**
     * 登录的处理逻辑。
     * @param username username参数。
     * @param password 密码参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity login(String username, String password);

    /**
     * profile的处理逻辑。
     * @param username username参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity profile(String username);

    /**
     * 更新Profile。
     * @param username username参数。
     * @param profile profile参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity updateProfile(String username, UserEntity profile);

    /**
     * 列出用户。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> listUsers();

    /**
     * 创建用户。
     * @param user 用户参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity createUser(UserEntity user);

    /**
     * 更新用户。
     * @param id 标识参数。
     * @param user 用户参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity updateUser(Integer id, UserEntity user);

    /**
     * 更新用户状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity updateUserStatus(Integer id, Integer status);

    /**
     * 重置用户密码。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity resetUserPassword(Integer id);

    /**
     * 删除用户。
     * @param id 标识参数。
     * @return UserEntity 类型的处理结果。
     */
    UserEntity deleteUser(Integer id);

    /**
     * batchUpdate状态的处理逻辑。
     * @param ids 标识集合参数。
     * @param status 状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> batchUpdateStatus(List<Integer> ids, Integer status);

    /**
     * batchDelete用户的处理逻辑。
     * @param ids 标识集合参数。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> batchDeleteUsers(List<Integer> ids);

    /**
     * batchUpdate角色的处理逻辑。
     * @param ids 标识集合参数。
     * @param roleName 角色名称参数。
     * @param roleCode 角色编码参数。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> batchUpdateRole(List<Integer> ids, String roleName, String roleCode);

    /**
     * batchUpdate部门的处理逻辑。
     * @param ids 标识集合参数。
     * @param department 部门参数。
     * @return 当前操作得到的记录集合。
     */
    List<UserEntity> batchUpdateDepartment(List<Integer> ids, String department);
}
