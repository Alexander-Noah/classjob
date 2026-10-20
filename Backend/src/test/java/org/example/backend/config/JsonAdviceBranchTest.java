package org.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import java.io.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonAdviceBranchTest {
    static class Message implements HttpInputMessage {
        final HttpHeaders headers=new HttpHeaders();final byte[] content;
        Message(String type,String content) {if(type!=null)headers.setContentType(MediaType.parseMediaType(type));this.content=content.getBytes(java.nio.charset.StandardCharsets.UTF_8);}
        public HttpHeaders getHeaders(){return headers;}
        public InputStream getBody(){return new ByteArrayInputStream(content);}
    }
    @Test void nonJsonBodiesArePreservedAndJsonSuffixReceivesTheSameValidation() throws Exception {
        var advice=new RequestInputValidationAdvice();
        for(String media:new String[]{null,"text/plain","application/octet-stream"}){var input=new Message(media,"not JSON");assertSame(input,advice.beforeBodyRead(input,null,String.class,null));}
        var input=new Message("application/problem+json","{\"title\":\"输入正常\"}");var output=advice.beforeBodyRead(input,null,Map.class,null);
        assertSame(input.getHeaders(),output.getHeaders());assertArrayEquals(input.content,output.getBody().readAllBytes());
        assertTrue(advice.supports(null,Map.class,null));
        for(String content:new String[]{""," ","{bad}","{}{}"})assertThrows(IllegalArgumentException.class,()->advice.beforeBodyRead(new Message("application/json",content),null,Map.class,null));
        var payload=Map.of("value",1);assertSame(payload,advice.afterBodyRead(payload,input,null,Map.class,null));
    }
}
