package org.example.backend.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Boundary contracts: valid values must survive and each invalid variant must be rejected. */
class InputValidationBranchTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void accountFieldsAcceptAbsentOptionalValuesButNeverBlankRequiredOrControlCharacters() {
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.profile(null));
        for (String value : Arrays.asList(null, "", " \t")) {
            assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.login(value, "legacy"));
            assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.login("existing", value));
        }
        for (String value : List.of("a".repeat(101), "admin\r", "admin\t", "admin\u007f"))
            assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.login(value, "legacy"));
        for (Integer status : Arrays.asList(null, 0, 1)) assertDoesNotThrow(() -> AccountInputValidation.status(status));
        for (Integer status : List.of(-1, 2, Integer.MAX_VALUE)) assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.status(status));
        for (String role : List.of("systemAdmin", "labAdmin", "academic")) assertDoesNotThrow(() -> AccountInputValidation.role(role));
        for (String role : Arrays.asList(null, "", "teacher", "maintenance")) assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.role(role));
        for (String contact : Arrays.asList(null, "", " ", "+86 (010) 1234-5678")) {
            UserEntity user = new UserEntity(); user.setPhone(contact);
            assertDoesNotThrow(() -> AccountInputValidation.profile(user));
        }
        UserEntity user = new UserEntity(); user.setEmail(" valid@example.edu ");
        AccountInputValidation.profile(user); assertEquals("valid@example.edu", user.getEmail());
        user.setRealName("a".repeat(101));assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.profile(user));
    }

    @Test void avatarSchemesHostAndCredentialsAreCheckedIndependently() {
        for (String avatar : Arrays.asList(null, "", " ", "/files/avatar/a.png", "avatars/a.png", "http://example.edu/a.png", "HTTPS://example.edu/a.png")) {
            UserEntity user = new UserEntity(); user.setAvatarUrl(avatar);
            assertDoesNotThrow(() -> AccountInputValidation.profile(user), String.valueOf(avatar));
        }
        for (String avatar : List.of("//example.edu/a.png", "a\\b.png", "javascript:alert(1)", "file:///tmp/a", "http:/missing-host", "https://user:pass@example.edu/a", "https://[bad-host/a")) {
            UserEntity user = new UserEntity(); user.setAvatarUrl(avatar);
            assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.profile(user), avatar);
        }
    }

    @Test void jsonStructureAllowsFinitePrimitivesInsideObjectsAndRejectsComplexityAttacks() throws Exception {
        for (String root : List.of("1", "\"text\"", "true", "null"))
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.readTree(root)));
        assertDoesNotThrow(() -> RequestInputValidation.json(json.readTree("{\"unknown\":null,\"flag\":true,\"ratio\":1.5,\"port\":null,\"notes\":\"a\\r\\n\\tb\"}")));
        assertDoesNotThrow(() -> RequestInputValidation.json(json.valueToTree(Map.of("metric", new BigDecimal("123.45")))));
        for (String key : List.of("prototype", "constructor", "bad\nkey"))
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.valueToTree(Map.of(key, "x"))));
        for (String value : List.of("-1.5", "2147483648", "\"3\"", "false"))
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.readTree("{\"quantity\":"+value+"}")));
        var huge = json.createArrayNode();
        for (int i=0;i<1000;i++) { var row=huge.addObject(); for(int j=0;j<51;j++) row.put("field"+j,j); }
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(huge));
        for (String field : List.of("status", "processStatus", "confirmStatus", "publishStatus", "tagType", "code", "deviceCode", "unit", "category", "courseName", "remark", "question", "unknown")) {
            int max=RequestInputValidation.stringLimit(field);
            assertDoesNotThrow(() -> RequestInputValidation.json(json.valueToTree(Map.of(field,"x".repeat(max)))));
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.valueToTree(Map.of(field,"x".repeat(max+1)))));
        }
        assertDoesNotThrow(() -> RequestInputValidation.json(json.valueToTree(Map.of("images",List.of(Map.of("url","x".repeat(20000)))))));
    }

    @Test void scalarTypesPreserveExactIntegerBooleanAndTextContracts() throws Exception {
        for (Class<?> type : List.of(int.class, Integer.class, long.class, Long.class)) {
            assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("1"),type));
            for(String invalid:List.of("true","1.1","\"1\"","9223372036854775808"))
                assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree(invalid),type));
        }
        for (Class<?> type : List.of(boolean.class, Boolean.class)) {
            assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("false"),type));
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree("0"),type));
        }
        assertDoesNotThrow(() -> RequestInputValidation.types(null, String.class));
        assertDoesNotThrow(() -> RequestInputValidation.types(json.nullNode(), String.class));
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("{}"), Object.class));
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("1"), Double.class));
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("\"safe\""), String.class));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree("{}"), String.class));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree("[]"), DevicesEntity.class));
        var collection=new TypeReference<List<Long>>(){}.getType();
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("[1,2]"),collection));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree("{}"),collection));
        var map=new TypeReference<Map<String,Integer>>(){}.getType();
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("{\"one\":1}"),map));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree("[]"),map));
        var parameterized=new TypeReference<Optional<String>>(){}.getType();
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("{}"),parameterized));
    }

    @Test void domainRangesExerciseBothBoundsNullsAndCollectionCycles() {
        var cyclic = new HashMap<String,Object>();cyclic.put("self",cyclic);cyclic.put("nothing",null);cyclic.put("ordinary","safe");
        assertDoesNotThrow(() -> RequestInputValidation.domain(cyclic));
        assertDoesNotThrow(() -> RequestInputValidation.domain(new UserEntity()));
        for(Integer capacity:Arrays.asList(null,0,100000)) {var lab=new LabEntity();lab.setLabName("Lab");lab.setCapacity(capacity);lab.setOpenStatus("");assertDoesNotThrow(()->RequestInputValidation.domain(lab));}
        for(Integer capacity:List.of(-1,100001)) {var lab=new LabEntity();lab.setLabName("Lab");lab.setCapacity(capacity);assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(lab));}
        for(Integer port:Arrays.asList(null,0,1,65535,65536)){var host=new TeacherHostEntity();host.setLabId(1L);host.setTeacherDeviceId("teacher");host.setHostIp("127.0.0.1");host.setPort(port);if(port!=null&&port>=1&&port<=65535)assertDoesNotThrow(()->RequestInputValidation.domain(host));else assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(host));}
        for(Long id:Arrays.asList(null,0L,-1L)){var client=new StudentClientEntity();client.setLabId(id);client.setStudentDeviceId("PC");assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(client));}
        for(Integer progress:Arrays.asList(null,0,100)){var repair=new RepairEntity();repair.setProgress(progress);assertDoesNotThrow(()->RequestInputValidation.domain(repair));}
        for(Integer quantity:Arrays.asList(null,1,1000000)){var device=new DevicesEntity();device.setDeviceName("PC");device.setQuantity(quantity);assertDoesNotThrow(()->RequestInputValidation.domain(device));}
        for(Integer quantity:List.of(0,1000001)){var device=new DevicesEntity();device.setDeviceName("PC");device.setQuantity(quantity);assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(device));}
        var fallback=new CourseEnvironmentEntity();fallback.setCourse("Fallback course");assertDoesNotThrow(()->RequestInputValidation.domain(fallback));fallback.setCourseName(" ");assertDoesNotThrow(()->RequestInputValidation.domain(fallback));
        for(Integer returned:Arrays.asList(null,0,2)){var loan=new ConsumableLoanEntity();loan.setReturnedQuantity(returned);assertDoesNotThrow(()->RequestInputValidation.domain(loan));loan.setQuantity(2);assertDoesNotThrow(()->RequestInputValidation.domain(loan));}
        for(String state:Arrays.asList(null,""," ","已取消")){var adjustment=new ScheduleAdjustmentEntity();adjustment.setCourse("Course");adjustment.setStatus(state);assertDoesNotThrow(()->RequestInputValidation.domain(adjustment));}
    }

    @Test void websocketProtocolValidatesEveryMessageStateAndFieldType() {
        for(String body:List.of("{\"type\":\"abort\"}","{\"type\":\"listen\",\"state\":\"start\",\"text\":\"safe\"}","{\"type\":\"listen\",\"state\":\"stop\"}"))assertDoesNotThrow(()->RequestInputValidation.websocketText(body));
        for(String body:Arrays.asList(null,"[]","{}","{\"type\":\"unknown\"}","{\"type\":\"hello\",\"state\":1}","{\"type\":\"hello\",\"text\":false}","x".repeat(RequestInputValidation.MAX_BODY_BYTES+1)))assertThrows(IllegalArgumentException.class,()->RequestInputValidation.websocketText(body));
    }
}
