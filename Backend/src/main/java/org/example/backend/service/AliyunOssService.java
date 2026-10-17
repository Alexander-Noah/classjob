/*
 * 阿里云对象存储能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AliyunOssService {
    /**
     * 上传头像。
     * @param file 文件参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, String> uploadAvatar(MultipartFile file);

    /**
     * 下载头像。
     * @param objectKey object键参数。
     * @return AvatarObject 类型的处理结果。
     */
    AvatarObject downloadAvatar(String objectKey);

    record AvatarObject(byte[] content, String contentType) {
    }
}
