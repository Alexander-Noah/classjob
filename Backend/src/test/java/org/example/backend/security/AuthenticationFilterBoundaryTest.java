package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterBoundaryTest {
    @Test void publicRoutesAndPreflightDoNotParseTokens() throws Exception {
        var service=mock(JwtService.class);
        for(String path:new String[]{"/auth/captcha","/public/image.png","/teacher-hosts/heartbeat","/student-clients/connect","/api/teacher-hosts/status","/api/student-clients/status"}) {
            boolean[] passed={false};
            new JwtAuthenticationFilter(service,true).doFilter(new MockHttpServletRequest("GET",path),new MockHttpServletResponse(),(req,res)->passed[0]=true);
            assertTrue(passed[0],path);
        }
        boolean[] passed={false};
        new JwtAuthenticationFilter(service,true).doFilter(new MockHttpServletRequest("OPTIONS","/users"),new MockHttpServletResponse(),(req,res)->passed[0]=true);
        assertTrue(passed[0]);verifyNoInteractions(service);
    }
    @Test void disabledFilterPassesBusinessRequest() throws Exception {
        var service=mock(JwtService.class);boolean[] passed={false};
        new JwtAuthenticationFilter(service,false).doFilter(new MockHttpServletRequest("GET","/users"),new MockHttpServletResponse(),(req,res)->passed[0]=true);
        assertTrue(passed[0]);verifyNoInteractions(service);
    }
    @Test void malformedAuthorizationHeadersNeverReachTokenParser() throws Exception {
        for(String header:new String[]{""," ","Basic abc","Bearer "}) {
            var service=mock(JwtService.class);var request=new MockHttpServletRequest("GET","/users");request.setServletPath("/users");request.addHeader("Authorization",header);
            var response=new MockHttpServletResponse();
            new JwtAuthenticationFilter(service,true).doFilter(request,response,(req,res)->fail("anonymous request reached chain"));
            assertEquals(401,response.getStatus());assertTrue(response.getContentAsString().contains("未授权"));verifyNoInteractions(service);
        }
    }
    @Test void bearerTokensTransferIdentityButExpiredAndSuspendedClaimsDoNot() throws Exception {
        var service=mock(JwtService.class);
        when(service.validateToken("valid")).thenReturn(new JwtService.JwtClaims("operator","labAdmin","42",Long.MAX_VALUE));
        when(service.validateToken("invalid")).thenThrow(new IllegalArgumentException("expired"));
        when(service.validateToken("teacher")).thenReturn(new JwtService.JwtClaims("old","teacher","4",Long.MAX_VALUE));
        for(String token:new String[]{"valid","invalid","teacher"}) {
            var request=new MockHttpServletRequest("GET","/users");request.addHeader("Authorization","bEaReR   "+token+"  ");
            var response=new MockHttpServletResponse();boolean[] passed={false};
            new JwtAuthenticationFilter(service,true).doFilter(request,response,(req,res)->passed[0]=true);
            assertEquals(token.equals("valid"),passed[0]);
            if(passed[0]) {assertEquals("operator",request.getAttribute(AuthenticationAttributes.USERNAME));assertEquals("42",request.getAttribute(AuthenticationAttributes.USER_ID));assertEquals("labAdmin",request.getAttribute(AuthenticationAttributes.ROLE));}
            else {assertEquals(401,response.getStatus());assertTrue(response.getContentAsString().contains(token.equals("teacher")?"角色已停用":"会话无效"));assertNull(request.getAttribute(AuthenticationAttributes.USERNAME));}
        }
    }
    @Test void policyNormalizesInputsAndProtectsSegmentBoundaries() {
        var policy=new SecurityAccessPolicy();
        assertTrue(policy.isAllowed(null," users//options/ "," labAdmin "));
        assertFalse(policy.isAllowed("GET","/users/options/extra","labAdmin"));
        assertFalse(policy.isAllowed("POST","/activation-codes/lab-bindings","labAdmin"));
        assertFalse(policy.isAllowed("GET","/activation-codes/abc/unbind-lab","labAdmin"));
        assertFalse(policy.isAllowed("POST","/activation-codes/a/b/unbind-lab","labAdmin"));
        assertTrue(policy.isAllowed("GET","/class-timetables/1","academic"));
        assertFalse(policy.isAllowed("GET","/class-timetables-extra","academic"));
        assertTrue(policy.isAllowed("GET","/notices/7/read",null));
        assertFalse(policy.isAllowed("GET",null,null));
        assertFalse(policy.isAllowed("GET","  ","unknown"));
        assertFalse(policy.isAllowed("GET","/dashboard-extra","unknown"));
    }
}
