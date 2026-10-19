package org.example.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestInputValidationTest {
    final ObjectMapper json = new ObjectMapper();
    @Test void validResourceStatesAndContactsAreAcceptedWhileInvalidStatesAreRejected() {
        var lab=new LabEntity();lab.setLabName("测试实验室");lab.setCapacity(30);lab.setDeviceCount(12);lab.setOpenStatus("开放");
        var adjustment=new ScheduleAdjustmentEntity();adjustment.setCourse("程序设计");adjustment.setFlowStep(2);adjustment.setStatus("审核中");
        assertDoesNotThrow(()->RequestInputValidation.domain(List.of(lab,adjustment)));
        lab.setOpenStatus("not-a-state");assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(lab));
        adjustment.setStatus("not-a-state");assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(adjustment));
        var user=new UserEntity();user.setPhone("+86 13800138000");user.setEmail(" test@example.com ");
        assertDoesNotThrow(()->AccountInputValidation.profile(user));assertEquals("test@example.com",user.getEmail());
        user.setPhone("not-a-number");assertThrows(IllegalArgumentException.class,()->AccountInputValidation.profile(user));
        assertThrows(IllegalArgumentException.class,()->AccountInputValidation.login("admin\nforged","safe-password"));
    }
    @Test void nestedBusinessPayloadsRequireNamesAndConsistentQuantities() {
        var stock=new ConsumableEntity();stock.setName("试剂");stock.setStock(0);stock.setWarnThreshold(2);
        var loan=new ConsumableLoanEntity();loan.setQuantity(3);loan.setReturnedQuantity(2);
        var student=new StudentClientEntity();student.setLabId(1L);student.setStudentDeviceId("PC-1");
        var notice=new NoticeEntity();notice.setTitle("安全提醒");notice.setContent("O'Brien <实验> 正文");
        var course=new CourseEnvironmentEntity();course.setCourseName("程序设计");
        var device=new DevicesEntity();device.setDeviceName("设备");device.setQuantity(1);device.setUsageHours(0);
        assertDoesNotThrow(() -> RequestInputValidation.domain(List.of(stock,loan,student,notice,course,device)));
        loan.setReturnedQuantity(4);assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(loan));
        stock.setStock(-1);assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(stock));
        student.setLabId(0L);assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(student));
        notice.setContent(" ");assertThrows(IllegalArgumentException.class,()->RequestInputValidation.domain(notice));
    }
    @Test void genericCollectionsAndEnumsDoNotAllowScalarCoercion() throws Exception {
        var type=new com.fasterxml.jackson.core.type.TypeReference<Map<String,List<Integer>>>(){}.getType();
        assertDoesNotThrow(()->RequestInputValidation.types(json.readTree("{\"ids\":[1,2]}"),type));
        for(String body:List.of("[]","{\"ids\":1}","{\"ids\":[1.5]}"))
            assertThrows(IllegalArgumentException.class,()->RequestInputValidation.types(json.readTree(body),type));
        assertDoesNotThrow(()->RequestInputValidation.types(json.readTree("\"PENDING\""),ReservationStatus.class));
        assertThrows(IllegalArgumentException.class,()->RequestInputValidation.types(json.readTree("1"),ReservationStatus.class));
        assertThrows(IllegalArgumentException.class,()->RequestInputValidation.types(json.readTree("\"true\""),Boolean.class));
    }
    @Test void fieldSpecificLimitsRejectOverflowWithoutRejectingOrdinaryText() {
        Map<String,Integer> limits=Map.of("password",128,"phone",40,"email",255,"department",160,"name",160,"title",255,"description",4000,"avatarUrl",500,"captchaId",36,"captchaCode",4);
        limits.forEach((field,max)->{
            assertDoesNotThrow(()->RequestInputValidation.json(json.valueToTree(Map.of(field,"a".repeat(max)))));
            assertThrows(IllegalArgumentException.class,()->RequestInputValidation.json(json.valueToTree(Map.of(field,"a".repeat(max+1)))));
        });
        var fields=new LinkedHashMap<String,String>();for(int i=0;i<201;i++)fields.put("field"+i,"value");
        assertThrows(IllegalArgumentException.class,()->RequestInputValidation.json(json.valueToTree(fields)));
        assertThrows(IllegalArgumentException.class,()->RequestInputValidation.json(json.valueToTree(Map.of("x".repeat(101),"value"))));
        assertThrows(IllegalArgumentException.class,()->RequestInputValidation.json(json.valueToTree(Map.of("metric",Double.POSITIVE_INFINITY))));
    }
    @Test void textAndMediaBoundsPreserveOrdinaryUserContent() throws Exception {
        assertDoesNotThrow(() -> RequestInputValidation.json(json.readTree("{\"name\":\"O'Brien 实验室\",\"description\":\"第一行\\n第二行 <测试>\"}")));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.valueToTree(Map.of("username", "x".repeat(101)))));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.valueToTree(Map.of("description", "bad\0value"))));
        assertDoesNotThrow(() -> RequestInputValidation.json(json.valueToTree(Map.of("html", "x".repeat(20000), "image_url", Map.of("url", "x".repeat(20000))))));
    }
    @Test void rejectsLargeBatchesDepthAndPrototypeKeys() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.valueToTree(Collections.nCopies(1001, 1))));
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(json.readTree("{\"__proto__\":{}}")));
        Object nested = Map.of(); for (int i=0; i<22; i++) nested=Map.of("child",nested);
        var tree=json.valueToTree(nested);
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.json(tree));
    }
    @Test void preventsTruncationOverflowScalarCoercionAndAliasAmbiguity() throws Exception {
        for (String payload : List.of("{\"labId\":1.5}","{\"labId\":9223372036854775808}","{\"labId\":\"1\"}","{\"quantity\":true}","{\"name\":\"a\",\"deviceName\":\"b\"}")) {
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.types(json.readTree(payload),DevicesEntity.class));
        }
        assertDoesNotThrow(() -> RequestInputValidation.types(json.readTree("{\"name\":\"设备\",\"labId\":1,\"quantity\":2}"),DevicesEntity.class));
    }
    @Test void checksDomainRulesEvenInNestedBatch() {
        var lab=new LabEntity();lab.setLabName("实验室");lab.setCapacity(-1);
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.domain(Map.of("records",List.of(lab))));
        var host=new TeacherHostEntity();host.setLabId(1L);host.setTeacherDeviceId("device");host.setHostIp("127.0.0.1");host.setPort(65536);
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.domain(host));
        var repair=new RepairEntity();repair.setProgress(101);
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.domain(repair));
        var adjustment=new ScheduleAdjustmentEntity();adjustment.setCourse("程序设计");adjustment.setStatus("arbitrary-status");
        assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.domain(adjustment));
    }
    @Test void accountsRejectInvalidStatusRoleContactAndLongPassword() {
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.login("admin","x".repeat(129)));
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.status(2));
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.role("unknown"));
        var user=new UserEntity();user.setEmail("not-an-email");
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.profile(user));
        user.setEmail(null);user.setAvatarUrl("javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> AccountInputValidation.profile(user));
        assertDoesNotThrow(() -> AccountInputValidation.login("existing-user","legacy"));
    }
    @Test void websocketPreservesPlainQuestionsButRejectsMalformedProtocol() {
        assertDoesNotThrow(() -> RequestInputValidation.websocketText("第一行\n第二行问题"));
        assertDoesNotThrow(() -> RequestInputValidation.websocketText("{\"type\":\"hello\"}"));
        assertDoesNotThrow(() -> RequestInputValidation.websocketText(json.writeValueAsString(Map.of("type","listen","state","detect","images",List.of(Map.of("dataUrl","x".repeat(20000)))))));
        for (String text : List.of("{bad}","{\"type\":\"hello\",\"type\":\"abort\"}","{\"type\":123}","{\"type\":\"listen\",\"state\":\"execute\"}","x".repeat(100001)))
            assertThrows(IllegalArgumentException.class, () -> RequestInputValidation.websocketText(text));
    }
}
