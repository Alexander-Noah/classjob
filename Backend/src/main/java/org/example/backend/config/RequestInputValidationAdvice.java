package org.example.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.security.RequestInputValidation;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import java.io.*;
import java.lang.reflect.Type;

/** Applies to every JSON controller body, including maps and nested batch requests. */
@RestControllerAdvice(basePackages = "org.example.backend.controller")
public class RequestInputValidationAdvice extends RequestBodyAdviceAdapter {
    private final ObjectMapper mapper = new ObjectMapper();
    public RequestInputValidationAdvice() {
        mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
                .maxNestingDepth(25).maxStringLength(8 * 1024 * 1024).maxNumberLength(100).build());
    }
    @Override public boolean supports(MethodParameter p, Type type, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter p, Type type,
            Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        MediaType media = input.getHeaders().getContentType();
        if (media == null || !(MediaType.APPLICATION_JSON.includes(media) || media.getSubtype().endsWith("+json"))) return input;
        byte[] bytes = input.getBody().readNBytes(RequestInputValidation.MAX_BODY_BYTES + 1);
        if (bytes.length > RequestInputValidation.MAX_BODY_BYTES)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "请求正文超过 12MB，请分批提交");
        try {
            var tree = mapper.readTree(bytes);
            if (tree == null) throw new IllegalArgumentException("请填写请求正文");
            RequestInputValidation.json(tree);
            RequestInputValidation.types(tree, type);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 格式错误、字段重复或请求结构过于复杂");
        }
        return new HttpInputMessage() {
            public HttpHeaders getHeaders() { return input.getHeaders(); }
            public InputStream getBody() { return new ByteArrayInputStream(bytes); }
        };
    }
    @Override public Object afterBodyRead(Object body, HttpInputMessage input, MethodParameter p, Type type,
            Class<? extends HttpMessageConverter<?>> converter) {
        RequestInputValidation.domain(body);
        return body;
    }
}
