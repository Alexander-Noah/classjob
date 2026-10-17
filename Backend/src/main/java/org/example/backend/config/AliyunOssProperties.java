/*
 * 阿里云对象存储配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-lab.oss")
public class AliyunOssProperties {
    private boolean enabled;
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String publicBaseUrl;
    private String avatarDir = "avatars";
}
