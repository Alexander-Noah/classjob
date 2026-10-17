/*
 * 头像资源回归测试；测试夹具与生产配置分离。
 * 覆盖场景：multipartParserAllowsTheFullAvatarLimitAndRequestOverhead。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.*;

class AvatarUploadLimitTest {
    /**
     * multipartParserAllowsTheFull头像上限And请求Overhead的处理逻辑。
     */
    @Test
    void multipartParserAllowsTheFullAvatarLimitAndRequestOverhead() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        var properties = yaml.getObject();
        assertNotNull(properties);
        long fileLimit = DataSize.parse(properties.getProperty("spring.servlet.multipart.max-file-size")).toBytes();
        long requestLimit = DataSize.parse(properties.getProperty("spring.servlet.multipart.max-request-size")).toBytes();
        assertEquals(2 * 1024 * 1024, fileLimit);
        assertTrue(requestLimit > fileLimit, "Multipart boundaries need extra space beyond the file size");
    }
}
