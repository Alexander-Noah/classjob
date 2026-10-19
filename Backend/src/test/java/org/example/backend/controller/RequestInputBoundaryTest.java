package org.example.backend.controller;

import org.example.backend.config.*;
import org.example.backend.service.DevicesService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestInputBoundaryTest {
    @Test void badJsonDuplicateKeysAndFractionalIdsNeverReachService() throws Exception {
        var service=mock(DevicesService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new DevicesController(service))
                .setControllerAdvice(new RequestInputValidationAdvice(),new GlobalExceptionHandler()).build();
        for(String body: java.util.List.of("{\"name\":\"a\",\"labId\":1.5}","{\"name\":\"a\",\"quantity\":-1}","{\"name\":\"a\",\"name\":\"b\"}","{\"name\":123}","{\"name\":\"\"}","{\"name\":\"a\"}{\"name\":\"b\"}")) {
            mvc.perform(post("/devices").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
        mvc.perform(post("/devices").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"实验设备 O'Brien\",\"labId\":1,\"quantity\":2}"))
                .andExpect(status().isOk());
        verify(service).InserterDevices(any());
    }
    @Test void queryAndPathChecksRunBeforeService() throws Exception {
        var service=mock(DevicesService.class);
        var registry=new org.springframework.web.servlet.config.annotation.InterceptorRegistry() {
            public java.util.List<Object> all() { return getInterceptors(); }
        };
        new RequestParameterValidation().addInterceptors(registry);
        var mvc=MockMvcBuilders.standaloneSetup(new DevicesController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(registry.all().toArray(org.springframework.web.servlet.HandlerInterceptor[]::new)).build();
        mvc.perform(get("/devices/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/page").param("pageNum","1.5")).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/page").param("pageSize","1001")).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/page").param("pageSize","0")).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/page").param("q","x".repeat(4097))).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/page").param("x".repeat(101),"value")).andExpect(status().isBadRequest());
        mvc.perform(get("/devices/9223372036854775808")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        mvc.perform(get("/devices/page").param("pageNum","1").param("pageSize","20")).andExpect(status().isOk());
        verify(service).pageDevices(any());
    }
    @Test void oversizedJsonIsRejectedBeforeParsingOrCallingBusinessLogic() throws Exception {
        var service=mock(DevicesService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new DevicesController(service))
                .setControllerAdvice(new RequestInputValidationAdvice(),new GlobalExceptionHandler()).build();
        mvc.perform(post("/devices").contentType(MediaType.APPLICATION_JSON)
                .content("x".repeat(org.example.backend.security.RequestInputValidation.MAX_BODY_BYTES+1)))
                .andExpect(status().isPayloadTooLarge());
        verifyNoInteractions(service);
    }
}

