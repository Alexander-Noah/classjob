/*
 * 文件上传的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/files/avatars、/files/avatar-proxy；完整路径由类级与方法级映射组合。
 * 主要协作组件：AliyunOssService。
 */
package org.example.backend.controller;

import org.example.backend.service.AliyunOssService.AvatarObject;
import org.example.backend.result.Result;
import org.example.backend.service.AliyunOssService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@CrossOrigin
@RestController
public class FileUploadController {
    private final AliyunOssService aliyunOssService;

    /**
     * 构造 FileUploadController，初始化当前实例。
     * @param aliyunOssService aliyunOss服务参数。
     */
    public FileUploadController(AliyunOssService aliyunOssService) {
        this.aliyunOssService = aliyunOssService;
    }

    /**
     * 上传头像。
     * @param file 文件参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/files/avatars")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success("上传头像成功", aliyunOssService.uploadAvatar(file));
    }

    /**
     * 获取头像。
     * @param objectKey object键参数。
     * @return ResponseEntity<byte[]> 类型的处理结果。
     */
    @GetMapping("/files/avatar-proxy")
    public ResponseEntity<byte[]> getAvatar(@RequestParam("objectKey") String objectKey) {
        AvatarObject avatarObject = aliyunOssService.downloadAvatar(objectKey);
        MediaType mediaType = parseMediaType(avatarObject.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header("Content-Disposition", MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)
                        ? "attachment; filename=\"avatar.bin\"" : "inline")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(avatarObject.content());
    }

    /**
     * 解析媒体类型。
     * @param contentType 内容类型参数。
     * @return MediaType 类型的处理结果。
     */
    private MediaType parseMediaType(String contentType) {
        return org.example.backend.security.SafeRasterMediaType.parse(contentType);
    }
}
