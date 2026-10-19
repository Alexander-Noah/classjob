package org.example.backend.security;

import jakarta.servlet.FilterChain;
import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.UserMapper;
import org.example.backend.service.impl.UserServiceImpl;
import org.example.backend.service.BusinessLoopService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeacherRoleSuspensionTest {
    /** 使用真实签名的历史教师令牌，验证过滤器阻止后续处理。 */
    @Test
    void oldTeacherTokenIsRejectedBeforeReachingController() throws Exception {
        JwtService jwt = new JwtService("test-only-strong-jwt-secret-with-40-plus-chars", 7200, "smart-lab");
        UserEntity user = teacher();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.setServletPath("/auth/profile");
        request.addHeader("Authorization", "Bearer " + jwt.generateToken(user));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, true).doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("角色已停用"));
        verifyNoInteractions(chain);
    }

    /** 登录和资料读取均拒绝教师角色，不改写密码或历史账号。 */
    @Test
    void teacherCannotLoginOrRefreshProfile() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordService passwords = mock(PasswordService.class);
        UserEntity user = teacher();
        when(mapper.login("teacher01")).thenReturn(user);
        when(mapper.profile("teacher01")).thenReturn(user);
        when(passwords.matches("test-pass", user.getPassword())).thenReturn(true);
        UserServiceImpl service = new UserServiceImpl(mapper, null, passwords);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service.login("teacher01", "test-pass")).getMessage().contains("角色已停用"));
        assertThrows(IllegalArgumentException.class, () -> service.profile("teacher01"));
        verify(mapper, never()).updatePassword(any(), any());
    }

    /** 创建与批量分配在写库前失败；空角色不能再默认为教师。 */
    @Test
    void cannotCreateAssignOrDefaultToTeacher() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(mapper, null, null);
        UserEntity user = teacher();
        user.setPassword("SafePass123");
        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
        assertThrows(IllegalArgumentException.class,
                () -> service.batchUpdateRole(java.util.List.of(1), "教师", "teacher"));
        assertThrows(IllegalArgumentException.class,
                () -> service.batchUpdateRole(java.util.List.of(1), "", ""));
        user.setRoleCode("");
        user.setRoleName("");
        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
        verifyNoInteractions(mapper);
    }

    /** 正常管理员会话继续通过，防止停用判断影响仍在使用的角色。 */
    @Test
    void administratorTokenStillReachesController() throws Exception {
        JwtService jwt = new JwtService("test-only-strong-jwt-secret-with-40-plus-chars", 7200, "smart-lab");
        UserEntity user = teacher();
        user.setRoleCode("labAdmin");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.setServletPath("/dashboard");
        request.addHeader("Authorization", "Bearer " + jwt.generateToken(user));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, true).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals("labAdmin", request.getAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE));
    }

    /** 历史教师账号仍允许改派活跃角色，不需要删除后重新创建。 */
    @Test
    void historicalTeacherCanBeReassignedToActiveRole() {
        UserMapper mapper = mock(UserMapper.class);
        UserEntity previous = teacher();
        UserEntity reassigned = teacher();
        reassigned.setRoleCode("labAdmin");
        reassigned.setRoleName("实验室管理员");
        when(mapper.getUser(17)).thenReturn(previous, reassigned);
        UserServiceImpl service = new UserServiceImpl(mapper, mock(BusinessLoopService.class), null);
        assertEquals("labAdmin", service.batchUpdateRole(java.util.List.of(17), "实验室管理员", "labAdmin").get(0).getRoleCode());
        verify(mapper).deleteUserRoles(17);
        verify(mapper).insertUserRole(17, "labAdmin");
    }

    /** 构造独立测试账号，不访问实际数据库。 */
    private UserEntity teacher() {
        UserEntity user = new UserEntity();
        user.setId(17);
        user.setUsername("teacher01");
        user.setRoleCode("teacher");
        user.setRoleName("教师");
        user.setPassword("stored-password");
        user.setStatus(1);
        return user;
    }
}
