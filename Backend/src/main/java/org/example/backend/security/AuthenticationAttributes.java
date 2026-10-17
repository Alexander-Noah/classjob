/*
 * 身份认证的服务端安全组件；调用方应保持身份认证与权限判断边界。
 */
package org.example.backend.security;

public final class AuthenticationAttributes {
    public static final String USERNAME = "authUsername";
    public static final String ROLE = "authRole";
    public static final String USER_ID = "authUserId";

    private AuthenticationAttributes() {
    }
}
