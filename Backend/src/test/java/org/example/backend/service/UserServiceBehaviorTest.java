package org.example.backend.service;

import org.example.backend.entity.UserEntity;
import org.example.backend.mapper.UserMapper;
import org.example.backend.security.PasswordService;
import org.example.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceBehaviorTest {
    final UserMapper mapper=mock(UserMapper.class);
    final BusinessLoopService loop=mock(BusinessLoopService.class);
    final PasswordService passwords=mock(PasswordService.class);
    final UserService service=new UserServiceImpl(mapper,loop,passwords);
    final Map<Integer,UserEntity> store=new HashMap<>();
    UserEntity user(int id){UserEntity user=new UserEntity();user.setId(id);user.setUsername("user"+id);user.setRealName("用户"+id);user.setPassword("old-hash");user.setStatus(1);user.setRoleCode("labAdmin");user.setRoleName("实验室管理员");user.setPermissions("permissions");user.setAvatarUrl("avatar");return user;}
    @BeforeEach void setup(){store.put(1,user(1));store.put(2,user(2));when(mapper.getUser(anyInt())).thenAnswer(call->store.get(call.getArgument(0)));when(mapper.updateUser(anyInt(),any())).thenAnswer(call->{if(!store.containsKey(call.getArgument(0)))return 0;UserEntity updated=call.getArgument(1);updated.setId(call.getArgument(0));store.put(call.getArgument(0),updated);return 1;});when(mapper.insertUser(any())).thenAnswer(call->{UserEntity user=call.getArgument(0);user.setId(3);store.put(3,user);return 1;});when(mapper.updateUserStatus(anyInt(),anyInt())).thenAnswer(call->{UserEntity user=store.get(call.getArgument(0));if(user==null)return 0;user.setStatus(call.getArgument(1));return 1;});when(mapper.deleteUser(anyInt())).thenAnswer(call->store.remove(call.getArgument(0))==null?0:1);when(mapper.updateUserDepartment(anyInt(),anyString())).thenReturn(1);when(passwords.encode(anyString())).thenAnswer(call->"encoded:"+call.getArgument(0));}
    @Test void loginMigratesLegacyHashesOnlyForEnabledMatchingAccounts(){
        UserEntity user=store.get(1);when(mapper.login("user1")).thenReturn(user);when(passwords.matches("valid",user.getPassword())).thenReturn(true);when(passwords.needsRehash(user.getPassword())).thenReturn(true);assertSame(user,service.login("user1","valid"));verify(mapper).updatePassword(1,"encoded:valid");assertEquals("encoded:valid",user.getPassword());assertNull(service.login("missing","valid"));assertNull(service.login("user1","wrong"));user.setStatus(0);assertNull(service.login("user1","valid"));
    }
    @Test void createRequiresUsernameEncodesPasswordAndSavesRoleAndAudit(){
        UserEntity input=new UserEntity();input.setUsername("new-user");input.setPassword("SafePass123");input.setRoleName("实验室管理员");UserEntity result=service.createUser(input);assertEquals("new-user",result.getUsername());assertEquals(result.getUsername(),result.getRealName());assertEquals(1,result.getStatus());assertEquals("labAdmin",result.getRoleCode());assertEquals("encoded:SafePass123",result.getPassword());verify(mapper).deleteUserRoles(3);verify(mapper).insertUserRole(3,"labAdmin");verify(loop).recordEvent(eq("user"),eq("create"),eq(result.getRealName()),eq("启用"),anyMap());
    }
    @ParameterizedTest @ValueSource(strings={"","short1","abcdefgh","123456789","admin123","qwerty123"}) void rejectsWeakInitialPasswords(String password){UserEntity input=user(3);input.setPassword(password);assertThrows(IllegalArgumentException.class,()->service.createUser(input));verify(mapper,never()).insertUser(any());}
    @Test void rejectsMissingUserPayloadAndRole(){assertThrows(IllegalArgumentException.class,()->service.createUser(null));UserEntity input=user(3);input.setPassword("SafePass123");input.setRoleCode("");input.setRoleName(null);assertThrows(IllegalArgumentException.class,()->service.createUser(input));}
    @Test void updatesRetainOmittedProtectedFieldsAndOnlyHashNewPlaintextPasswords(){
        UserEntity input=new UserEntity();input.setRoleCode("labAdmin");UserEntity result=service.updateUser(1,input);assertEquals("user1",result.getUsername());assertEquals("old-hash",result.getPassword());assertEquals("permissions",result.getPermissions());assertEquals("avatar",result.getAvatarUrl());assertEquals(1,result.getStatus());verify(passwords,never()).encode(anyString());
        UserEntity replacement=user(1);replacement.setPassword("SafePass123");assertEquals("encoded:SafePass123",service.updateUser(1,replacement).getPassword());replacement.setPassword("EncodedPass123");when(passwords.isEncoded("EncodedPass123")).thenReturn(true);service.updateUser(1,replacement);verify(passwords).encode("EncodedPass123");assertThrows(IllegalArgumentException.class,()->service.updateUser(99,user(99)));
    }
    @Test void selfProfileCannotChangeUsernameRolePermissionsOrStatus(){
        UserEntity existing=store.get(1);existing.setDepartment("旧部门");when(mapper.profile("user1")).thenReturn(existing);UserEntity input=new UserEntity();input.setUsername("admin");input.setRoleCode("systemAdmin");input.setPermissions("all");input.setStatus(0);input.setRealName(" 新姓名 ");input.setDepartment(" 新部门 ");input.setPhone(" ");UserEntity result=service.updateProfile("user1",input);assertEquals("user1",result.getUsername());assertEquals("labAdmin",result.getRoleCode());assertEquals("permissions",result.getPermissions());assertEquals(1,result.getStatus());assertEquals("新姓名",result.getRealName());assertEquals("新部门",result.getDepartment());assertEquals("",result.getPhone());assertThrows(IllegalArgumentException.class,()->service.updateProfile("",input));assertThrows(IllegalArgumentException.class,()->service.updateProfile("missing",input));
    }
    @Test void statusResetAndBatchDepartmentUseNormalizedDistinctIdentifiers(){
        assertEquals(1,service.updateUserStatus(1,null).getStatus());assertEquals(0,service.updateUserStatus(1,0).getStatus());service.resetUserPassword(1);verify(passwords).encode("Reset1234");verify(mapper).updatePassword(1,"encoded:Reset1234");assertEquals(2,service.batchUpdateStatus(List.of(1,2,1),1).size());assertEquals(2,service.batchUpdateDepartment(List.of(1,2),"").size());verify(mapper).updateUserDepartment(1,"未设置");assertThrows(IllegalArgumentException.class,()->service.updateUserStatus(99,1));when(mapper.updateUserDepartment(99,"新部门")).thenReturn(0);assertThrows(IllegalArgumentException.class,()->service.batchUpdateDepartment(List.of(99),"新部门"));assertThrows(IllegalArgumentException.class,()->service.batchUpdateStatus(null,1));assertThrows(IllegalArgumentException.class,()->service.batchDeleteUsers(List.of()));
    }
    @Test void deletionRejectsBuiltInLastAdministratorAndBusinessReferences(){
        UserEntity user=store.get(1);user.setUsername("sysadmin");assertThrows(IllegalArgumentException.class,()->service.deleteUser(1));user.setUsername("user1");user.setRoleCode("systemAdmin");when(mapper.countSystemAdmins()).thenReturn(1);assertThrows(IllegalArgumentException.class,()->service.deleteUser(1));user.setRoleCode("labAdmin");when(mapper.countUserBusinessReferences(1)).thenReturn(1);assertTrue(assertThrows(IllegalArgumentException.class,()->service.deleteUser(1)).getMessage().contains("用户1"));verify(mapper,never()).deleteUser(anyInt());
    }
    @Test void deletionValidatesEntireBatchBeforeRemovingAnyUser(){
        when(mapper.countUserBusinessReferences(2)).thenReturn(1);assertThrows(IllegalArgumentException.class,()->service.batchDeleteUsers(List.of(1,2)));verify(mapper,never()).deleteUser(anyInt());when(mapper.countUserBusinessReferences(2)).thenReturn(0);assertEquals(2,service.batchDeleteUsers(List.of(1,2,1)).size());verify(mapper).deleteUserRoles(1);verify(mapper).deleteUser(1);assertThrows(IllegalArgumentException.class,()->service.deleteUser(99));
    }
    @Test void batchRoleProtectsLastAdministratorAndAssignsRoleSpecificPermissions(){
        store.get(1).setRoleCode("systemAdmin");when(mapper.countSystemAdmins()).thenReturn(1);assertThrows(IllegalArgumentException.class,()->service.batchUpdateRole(List.of(1),"实验室管理员",""));store.get(1).setRoleCode("labAdmin");assertEquals(1,service.batchUpdateRole(List.of(1),"实验室管理员","").size());verify(mapper).updateUserPermissions(eq(1),contains("资源管理"));service.batchUpdateRole(List.of(2),"系统管理员","");verify(mapper).updateUserPermissions(eq(2),contains("平台治理"));assertThrows(IllegalArgumentException.class,()->service.batchUpdateRole(List.of(1),"",""));
    }
    @Test void listAndProfileReturnEmptyOrMissingResultsWithoutErrors(){when(mapper.listUsers()).thenReturn(null);assertEquals(List.of(),service.listUsers());when(mapper.listUsers()).thenReturn(List.of(store.get(1)));assertEquals(1,service.listUsers().size());assertNull(service.profile("missing"));when(mapper.profile("user1")).thenReturn(store.get(1));assertEquals("user1",service.profile("user1").getUsername());}
}
