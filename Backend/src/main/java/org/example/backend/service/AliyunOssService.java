/*
 * 阿里云对象存储能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AliyunOssService {
    Map<String, String> uploadAvatar(MultipartFile file);

    AvatarObject downloadAvatar(String objectKey);

    record AvatarObject(byte[] content, String contentType) {
    }
}
