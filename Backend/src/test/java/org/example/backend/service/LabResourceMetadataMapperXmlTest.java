/*
 * 实验室资源回归测试；测试夹具与生产配置分离。
 * 覆盖场景：performsOneBatchQueryPerMetadataSource。
 */
package org.example.backend.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LabResourceMetadataMapperXmlTest {
    /**
     * performsOneBatch查询PerMetadata来源的处理逻辑。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void performsOneBatchQueryPerMetadataSource() throws IOException {
        String xml = mapperXml();

        assertTrue(xml.contains("from sys_user"));
        assertTrue(xml.contains("from device"));
        assertTrue(xml.contains("collection=\"managerUserIds\""));
        assertTrue(xml.contains("collection=\"labIds\""));
        assertTrue(xml.contains("group by lab_id"));
    }

    /**
     * 数据映射器Xml的处理逻辑。
     * @return String 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
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
