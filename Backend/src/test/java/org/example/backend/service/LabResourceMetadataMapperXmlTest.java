package org.example.backend.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LabResourceMetadataMapperXmlTest {
    @Test
    void performsOneBatchQueryPerMetadataSource() throws IOException {
        String xml = mapperXml();

        assertTrue(xml.contains("from sys_user"));
        assertTrue(xml.contains("from device"));
        assertTrue(xml.contains("collection=\"managerUserIds\""));
        assertTrue(xml.contains("collection=\"labIds\""));
        assertTrue(xml.contains("group by lab_id"));
    }

    private String mapperXml() throws IOException {
        try (var input = getClass().getResourceAsStream("/mapper/LabResourceMetadataMapper.xml")) {
            if (input == null) {
                throw new IOException("LabResourceMetadataMapper.xml not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
}
