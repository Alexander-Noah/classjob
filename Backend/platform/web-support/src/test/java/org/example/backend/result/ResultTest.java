package org.example.backend.result;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultTest {
    @Test
    void createsCompatibleSuccessAndErrorEnvelopes() {
        Result success = Result.success(List.of("row"));
        assertEquals(200, success.getCode());
        assertEquals("操作成功", success.getMessage());
        assertEquals(List.of("row"), success.getData());

        Result error = Result.error("failed");
        assertEquals(-1, error.getCode());
        assertEquals("failed", error.getMessage());
        assertEquals(Map.of(), error.getData());
    }

    @Test
    void preservesEmptySuccessAndCustomFactories() {
        Result empty = Result.success();
        assertEquals(200, empty.getCode());
        assertEquals("操作成功", empty.getMessage());
        assertEquals(Map.of(), empty.getData());

        Result customSuccess = Result.success("accepted", "payload");
        assertEquals(200, customSuccess.getCode());
        assertEquals("accepted", customSuccess.getMessage());
        assertEquals("payload", customSuccess.getData());

        Result customError = Result.error(409, "conflict");
        assertEquals(409, customError.getCode());
        assertEquals("conflict", customError.getMessage());
        assertEquals(Map.of(), customError.getData());
    }
}
