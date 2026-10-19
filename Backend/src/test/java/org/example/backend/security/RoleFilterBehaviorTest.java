package org.example.backend.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RoleFilterBehaviorTest {
    @Test void missingRoleDefersAuthenticationWhileAuthorizedRoleReachesBusinessChain() throws Exception{RoleAuthorizationFilter filter=new RoleAuthorizationFilter();FilterChain chain=mock(FilterChain.class);MockHttpServletRequest noRole=new MockHttpServletRequest("GET","/labs");MockHttpServletResponse response=new MockHttpServletResponse();filter.doFilter(noRole,response,chain);verify(chain).doFilter(noRole,response);MockHttpServletRequest admin=new MockHttpServletRequest("GET","/users");admin.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE,"systemAdmin");filter.doFilter(admin,new MockHttpServletResponse(),chain);verify(chain).doFilter(eq(admin),any());}
    @Test void forbiddenRoleCannotInvokeBusinessAndServletPathIsPreferred() throws Exception{RoleAuthorizationFilter filter=new RoleAuthorizationFilter();FilterChain chain=mock(FilterChain.class);MockHttpServletRequest request=new MockHttpServletRequest("GET","/labs");request.setServletPath("/users");request.setAttribute(JwtAuthenticationFilter.AUTH_ROLE_ATTRIBUTE,"labAdmin");MockHttpServletResponse response=new MockHttpServletResponse();filter.doFilter(request,response,chain);assertEquals(403,response.getStatus());assertTrue(response.getContentAsString().contains("Forbidden"));verifyNoInteractions(chain);}
}
