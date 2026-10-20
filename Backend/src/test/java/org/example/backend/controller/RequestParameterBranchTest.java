package org.example.backend.controller;

import org.example.backend.config.*;
import org.example.backend.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestParameterBranchTest {
    @RestController static class BoundaryController {
        int reached;
        @GetMapping("/boundary/int/{id}") Result integer(@PathVariable int id) {reached++;return Result.success(id);}
        @GetMapping("/boundary/integer/{identifier}") Result integerObject(@PathVariable(name="identifier") Integer id) {reached++;return Result.success(id);}
        @GetMapping("/boundary/long/{identifier}") Result longObject(@PathVariable("identifier") Long id) {reached++;return Result.success(id);}
        @GetMapping("/boundary/string/{id}") Result string(@PathVariable String id) {reached++;return Result.success(id);}
        @GetMapping("/boundary/query") Result query() {reached++;return Result.success();}
    }
    private MockMvc mvc(BoundaryController controller) {
        var registry=new org.springframework.web.servlet.config.annotation.InterceptorRegistry(){public List<Object> all(){return getInterceptors();}};
        new RequestParameterValidation().addInterceptors(registry);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(registry.all().toArray(org.springframework.web.servlet.HandlerInterceptor[]::new)).build();
    }
    @Test void paginationValidatesPageNumberAndPageSizeBoundsIndependently() throws Exception {
        var controller=new BoundaryController();var mvc=mvc(controller);
        for(String key:List.of("page","pageNum","pageSize","size")) {
            for(String invalid:List.of("0","-1","1.5","not-number","2147483648",key.contains("Size")||key.equals("size")?"1001":"1000001"))
                mvc.perform(get("/boundary/query").param(key,invalid)).andExpect(status().isBadRequest());
        }
        assertEquals(0,controller.reached);
        mvc.perform(get("/boundary/query").param("page","1000000").param("pageNum","1").param("pageSize","1000").param("size","1")).andExpect(status().isOk());
        mvc.perform(get("/boundary/query").param("page","").param("keyword","实验室")).andExpect(status().isOk());
        assertEquals(2,controller.reached);
    }
    @Test void numericPathsRejectOverflowZeroNegativeAndMalformedValuesButTextIdsRemainSupported() throws Exception {
        var controller=new BoundaryController();var mvc=mvc(controller);
        for(String kind:List.of("int","integer","long"))for(String invalid:List.of("0","-1","1.5","invalid","9223372036854775808"))
            mvc.perform(get("/boundary/"+kind+"/"+invalid)).andExpect(status().isBadRequest());
        for(String kind:List.of("int","integer"))mvc.perform(get("/boundary/"+kind+"/2147483648")).andExpect(status().isBadRequest());
        assertEquals(0,controller.reached);
        mvc.perform(get("/boundary/int/2147483647")).andExpect(status().isOk());
        mvc.perform(get("/boundary/integer/1")).andExpect(status().isOk());
        mvc.perform(get("/boundary/long/9223372036854775807")).andExpect(status().isOk());
        mvc.perform(get("/boundary/string/RO-2026")).andExpect(status().isOk());
        assertEquals(4,controller.reached);
    }
    @Test void queryCountsDuplicateValuesAndControlCharactersAreBoundedBeforeController() throws Exception {
        var controller=new BoundaryController();var mvc=mvc(controller);
        var tooMany=get("/boundary/query");for(int i=0;i<101;i++)tooMany.param("q"+i,"x");mvc.perform(tooMany).andExpect(status().isBadRequest());
        mvc.perform(get("/boundary/query").param("q",Collections.nCopies(101,"x").toArray(String[]::new))).andExpect(status().isBadRequest());
        for(String invalid:List.of("line\nforged","bad\tvalue","bad\rvalue","bad\u007fvalue"))mvc.perform(get("/boundary/query").param("q",invalid)).andExpect(status().isBadRequest());
        assertEquals(0,controller.reached);
        mvc.perform(get("/boundary/query").param("q","x".repeat(4096))).andExpect(status().isOk());
        mvc.perform(get("/boundary/query").param("q",Collections.nCopies(100,"x").toArray(String[]::new))).andExpect(status().isOk());
        assertEquals(2,controller.reached);
    }
}
