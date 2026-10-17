/*
 * 安全策略回归测试；测试夹具与生产配置分离。
 * 覆盖场景：acceptsAvatarsAboveTheOldOneMegabyteLimitThroughTwoMegabytes、rejectsAvatarsOverTwoMegabytes、rejectsDisguisedImageUpload、rejectsContentTypeAndExtensionMismatch 等。
 */
package org.example.backend.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunOssServiceImplSecurityTest {
    /**
     * accepts头像AboveTheOldOneMegabyte上限ThroughTwoMegabytes的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void acceptsAvatarsAboveTheOldOneMegabyteLimitThroughTwoMegabytes() throws Exception {
        Object service = newAliyunOssService();
        for (int size : new int[]{1536 * 1024, 2 * 1024 * 1024}) {
            MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                    java.util.Arrays.copyOf(validPngBytes(), size));
            assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> invokeValidateFile(service, file));
        }
    }

    /**
     * rejects头像OverTwoMegabytes的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void rejectsAvatarsOverTwoMegabytes() throws Exception {
        Object service = newAliyunOssService();
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                java.util.Arrays.copyOf(validPngBytes(), 2 * 1024 * 1024 + 1));
        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> invokeValidateFile(service, file));
    }

    /**
     * rejectsDisguisedImageUpload的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void rejectsDisguisedImageUpload() throws Exception {
        Object service = newAliyunOssService();
        MultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "not-a-real-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> invokeValidateFile(service, file));
    }

    /**
     * rejects内容类型AndExtensionMismatch的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void rejectsContentTypeAndExtensionMismatch() throws Exception {
        Object service = newAliyunOssService();
        MultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/png",
                validPngBytes()
        );

        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> invokeValidateFile(service, file));
    }

    /**
     * accepts有效PngSignature的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void acceptsValidPngSignature() throws Exception {
        Object service = newAliyunOssService();
        MultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                validPngBytes()
        );

        assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> invokeValidateFile(service, file));
    }

    /**
     * newAliyunOss服务的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newAliyunOssService() throws Exception {
        Class<?> propertiesClass = Class.forName("org.example.backend.config.AliyunOssProperties");
        Object properties = propertiesClass.getConstructor().newInstance();
        return Class.forName("org.example.backend.service.impl.AliyunOssServiceImpl")
                .getConstructor(propertiesClass)
                .newInstance(properties);
    }

    /**
     * 调用Validate文件。
     * @param service 服务参数。
     * @param file 文件参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object invokeValidateFile(Object service, MultipartFile file) throws Exception {
        try {
            Method method = service.getClass().getDeclaredMethod("validateFile", MultipartFile.class);
            method.setAccessible(true);
            return method.invoke(service, file);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    /**
     * 有效PngBytes的处理逻辑。
     * @return byte[] 类型的处理结果。
     */
    private byte[] validPngBytes() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
        };
    }
}
