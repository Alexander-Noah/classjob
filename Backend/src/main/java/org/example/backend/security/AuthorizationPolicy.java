/*
 * 操作授权契约，负责“是否允许操作”，不承担令牌验证或身份认证。
 */
package org.example.backend.security;

public interface AuthorizationPolicy {
    /**
     * @param authority 已由调用方取得的权限或身份标识，具体编码由实现约定
     * @param action 待检查的操作标识，不能仅依据前端菜单可见性跳过检查
     * @return 实现策略明确允许时返回 true；本接口不规定空值或未知操作的默认行为
     */
    boolean isAllowed(String authority, String action);
}
