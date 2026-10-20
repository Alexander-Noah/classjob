package org.example.backend.controller;

import org.example.backend.entity.UserEntity;
import org.example.backend.result.Result;
import org.example.backend.security.JwtAuthenticationFilter;
import org.example.backend.security.JwtService;
import org.example.backend.security.LoginAttemptLimiter;
import org.example.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerBehaviorTest {
    final UserService users=mock(UserService.class);
    final JwtService jwt=mock(JwtService.class);
    final LoginAttemptLimiter limiter=mock(LoginAttemptLimiter.class);
    final org.example.backend.security.LoginCaptchaService captcha=mock(org.example.backend.security.LoginCaptchaService.class);
    { when(captcha.verify(any(),any())).thenReturn(true); }
    final UserController controller=new UserController(users,jwt,limiter,captcha);
    UserEntity user(){UserEntity user=new UserEntity();user.setId(7);user.setUsername("teacher");user.setPassword("secret-hash");user.setRoleCode("teacher");user.setRealName("教师");user.setRoleName("任课教师");user.setDepartment("计算机");user.setStatus(1);return user;}
    MockHttpServletRequest request(String username,String role){MockHttpServletRequest request=new MockHttpServletRequest();if(username!=null)request.setAttribute(JwtAuthenticationFilter.AUTH_USERNAME_ATTRIBUTE,username);if(role!=null)request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE,role);return request;}
    @Test void successfulLoginClearsFailuresAndReturnsTokenWithoutPassword(){
        UserEntity user=user();MockHttpServletRequest request=request(null,null);when(users.login("teacher","pw")).thenReturn(user);when(jwt.generateToken(user)).thenReturn("signed-token");
        Result result=controller.login(Map.of("username","teacher","password","pw"),request);
        assertEquals(200,result.getCode());Map<?,?> data=(Map<?,?>)result.getData();assertEquals("signed-token",data.get("token"));assertEquals("teacher",data.get("role"));assertNull(((UserEntity)data.get("user")).getPassword());
        var order=inOrder(limiter,users,jwt);order.verify(limiter).assertAllowed("teacher",request);order.verify(users).login("teacher","pw");order.verify(limiter).recordSuccess("teacher",request);order.verify(jwt).generateToken(user);
    }
    @Test void invalidCaptchaNeverChecksPasswordOrIssuesToken(){
        when(captcha.verify(any(),any())).thenReturn(false);
        MockHttpServletRequest request=request(null,null);
        assertEquals(-1,controller.login(Map.of("username","teacher","password","pw"),request).getCode());
        verifyNoInteractions(users,jwt);
        verify(limiter).recordFailure("teacher",request);
    }
    @Test void failedAndMissingLoginPayloadsRecordFailuresWithoutIssuingToken(){
        MockHttpServletRequest request=request(null,null);assertThrows(IllegalArgumentException.class,()->controller.login(null,request));verifyNoInteractions(users,limiter,jwt);
        doThrow(new IllegalArgumentException("locked")).when(limiter).assertAllowed("locked",request);
        assertThrows(IllegalArgumentException.class,()->controller.login(Map.of("username","locked","password","pw"),request));verify(users,never()).login("locked","pw");
    }
    @Test void profilesEnforceIdentityAndAdministratorAccess(){
        when(users.profile(anyString())).thenAnswer(call->user());
        assertEquals(401,controller.profile(null,request(null,null)).getCode());
        assertEquals(403,controller.profile("other",request("teacher","teacher")).getCode());
        assertNull(((UserEntity)controller.profile("",request("teacher","teacher")).getData()).getPassword());
        assertEquals(200,controller.profile("other",request("admin","systemAdmin")).getCode());verify(users).profile("other");
        when(users.profile("missing")).thenReturn(null);assertEquals(-1,controller.profile("missing",request("admin","systemAdmin")).getCode());
    }
    @Test void profileUpdateUsesAuthenticatedUsernameAndSanitizesResults(){
        UserEntity input=user();input.setUsername("someone-else");when(users.updateProfile("teacher",input)).thenReturn(user());
        assertEquals(401,controller.updateProfile(input,request(null,null)).getCode());
        assertNull(((UserEntity)controller.updateProfile(input,request("teacher","teacher")).getData()).getPassword());verify(users).updateProfile("teacher",input);
        assertEquals(200,controller.logout().getCode());
    }
    @Test void listingAndCrudNeverExposePasswordHashes(){
        when(users.listUsers()).thenAnswer(call->List.of(user()));
        assertNull(((UserEntity)((List<?>)controller.listUsers().getData()).get(0)).getPassword());
        Map<?,?> option=(Map<?,?>)((List<?>)controller.listUserOptions().getData()).get(0);assertEquals(7,option.get("id"));assertEquals("教师",option.get("realName"));assertFalse(option.containsKey("password"));
        UserEntity input=user();when(users.createUser(input)).thenAnswer(call->user());when(users.updateUser(7,input)).thenAnswer(call->user());when(users.updateUserStatus(eq(7),nullable(Integer.class))).thenAnswer(call->user());when(users.deleteUser(7)).thenAnswer(call->user());when(users.resetUserPassword(7)).thenAnswer(call->user());
        for(Result result:List.of(controller.createUser(input),controller.updateUser(7,input),controller.updateUserStatus(7,null),controller.updateUserStatus(7,Map.of("status",0)),controller.deleteUser(7),controller.resetUserPassword(7))) {assertEquals(200,result.getCode());assertNull(((UserEntity)result.getData()).getPassword());}
        verify(users).updateUserStatus(7,1);verify(users).updateUserStatus(7,0);
        when(users.createUser(input)).thenReturn(null);assertNull(controller.createUser(input).getData());
        when(users.listUsers()).thenReturn(null);assertNull(controller.listUsers().getData());
    }
    @Test void batchPayloadsDeduplicateValidIdentifiersAndHandleMissingFields(){
        List<Integer> ids=List.of(7,8);Map<String,Object> payload=new HashMap<>();payload.put("ids",Arrays.asList(7,"8",7L));payload.put("role","教师");payload.put("roleCode","teacher");payload.put("department",42);
        when(users.batchUpdateStatus(anyList(),anyInt())).thenAnswer(call->List.of(user()));when(users.batchDeleteUsers(anyList())).thenAnswer(call->List.of(user()));when(users.batchUpdateRole(anyList(),anyString(),anyString())).thenAnswer(call->List.of(user()));when(users.batchUpdateDepartment(anyList(),anyString())).thenAnswer(call->List.of(user()));
        for(Result result:List.of(controller.batchEnableUsers(payload),controller.batchDisableUsers(payload),controller.batchDeleteUsers(payload),controller.batchUpdateRole(payload),controller.batchUpdateDepartment(payload)))assertNull(((UserEntity)((List<?>)result.getData()).get(0)).getPassword());
        verify(users).batchUpdateStatus(ids,1);verify(users).batchUpdateStatus(ids,0);verify(users).batchDeleteUsers(ids);verify(users).batchUpdateRole(ids,"教师","teacher");verify(users).batchUpdateDepartment(ids,"42");
        assertThrows(IllegalArgumentException.class,()->controller.batchUpdateRole(null));assertThrows(IllegalArgumentException.class,()->controller.batchDeleteUsers(Map.of("ids","invalid")));
    }

    @Test void wrongPasswordRecordsFailureAndCaptchaResponseDisablesCaching() {
        MockHttpServletRequest request=request(null,null);
        Result denied=controller.login(Map.of("username"," known ","password","wrong"),request);
        assertEquals(-1,denied.getCode());verify(limiter).recordFailure("known",request);verify(jwt,never()).generateToken(any());
        var response=new org.springframework.mock.web.MockHttpServletResponse();when(captcha.create()).thenReturn(Map.of("captchaId","id","image","data:image/png;base64,AA=="));
        assertEquals(200,controller.captcha(response).getCode());assertEquals("no-store",response.getHeader("Cache-Control"));
    }
    @Test void blankSessionAndMalformedBatchIdsAreRejectedBeforeBusinessCalls() {
        assertEquals(401,controller.profile(" ",request(" ","labAdmin")).getCode());
        assertEquals(401,controller.updateProfile(new UserEntity(),request(" ","labAdmin")).getCode());
        for(List<?> ids:List.of(List.of(),Collections.nCopies(1001,1),Arrays.asList(1,null),List.of(0),List.of(-1),List.of(1.5),List.of("2147483648"),List.of("not-id")))
            assertThrows(IllegalArgumentException.class,()->controller.batchDeleteUsers(Map.of("ids",ids)));
        verifyNoInteractions(users);
    }
}
