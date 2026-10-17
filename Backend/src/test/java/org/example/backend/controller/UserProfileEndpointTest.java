/*
 * 用户回归测试；测试夹具与生产配置分离。
 * 覆盖场景：exposesSelfServiceProfileUpdateEndpoint。
 */
package org.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileEndpointTest {
    /**
     * exposes当前实例服务ProfileUpdate端点的处理逻辑。
     */
    @Test
    void exposesSelfServiceProfileUpdateEndpoint() {
        boolean hasProfileUpdateEndpoint = Arrays.stream(controllerClass().getDeclaredMethods())
                .map(/** 将当前条目转换为目标结果。 入参：method。 */ method -> method.getAnnotation(PutMapping.class))
                .filter(/** 判断当前条目是否符合筛选条件。 入参：annotation。 */ annotation -> annotation != null)
                .flatMap(/** 转换并展开当前条目。 入参：annotation。 */ annotation -> Arrays.stream(annotation.value()))
                .anyMatch("/auth/profile"::equals);

        assertTrue(hasProfileUpdateEndpoint, "current user profile edits must use PUT /auth/profile");
    }

    /**
     * controllerClass的处理逻辑。
     * @return Class<?> 类型的处理结果。
     */
    private Class<?> controllerClass() {
        try {
            return Class.forName("org.example.backend.controller.UserController");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
