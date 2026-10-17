/*
 * 教师主机回归测试；测试夹具与生产配置分离。
 * 覆盖场景：springCanInstantiateTeacherHostServiceWithMapperDependency、heartbeatRequiresPositiveLabId、currentHostTreatsExpiredHeartbeatAsOffline。
 */
package org.example.backend.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeacherHostServiceImplTest {
    /**
     * springCanInstantiate教师主机服务With数据映射器Dependency的处理逻辑。
     */
    @Test
    void springCanInstantiateTeacherHostServiceWithMapperDependency() {
        assertDoesNotThrow(/** 处理 assertDoesNotThrow 调用中的回调逻辑。 */ () -> {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                Class<?> mapperClass = Class.forName("org.example.backend.mapper.TeacherHostMapper");
                Class<?> serviceClass = Class.forName("org.example.backend.service.impl.TeacherHostServiceImpl");
                Object mapper = newFakeTeacherHostMapper();

                context.getDefaultListableBeanFactory().registerSingleton("teacherHostMapper", mapper);
                context.register(serviceClass);
                context.refresh();

                assertNotNull(context.getBean(serviceClass));
            }
        });
    }

    /**
     * heartbeatRequiresPositive实验室标识的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void heartbeatRequiresPositiveLabId() throws Exception {
        Object mapper = newFakeTeacherHostMapper();
        Object service = newTeacherHostService(mapper, 15);
        Object request = newTeacherHostEntity();
        invokeSetter(request, "setLabId", Long.class, 0L);
        invokeSetter(request, "setTeacherDeviceId", String.class, "teacher-1");
        invokeSetter(request, "setHostIp", String.class, "192.168.1.10");
        invokeSetter(request, "setPort", Integer.class, 8765);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> invokeRuntime(service, "heartbeat", new Class<?>[]{request.getClass()}, request)
        );

        assertEquals("labId must be positive", error.getMessage());
    }

    /**
     * 当前主机TreatsExpiredHeartbeatAsOffline的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void currentHostTreatsExpiredHeartbeatAsOffline() throws Exception {
        FakeTeacherHostMapper handler = new FakeTeacherHostMapper();
        Object stale = newTeacherHostEntity();
        invokeSetter(stale, "setLabId", Long.class, 12L);
        invokeSetter(stale, "setTeacherDeviceId", String.class, "teacher-1");
        invokeSetter(stale, "setHostIp", String.class, "192.168.1.10");
        invokeSetter(stale, "setPort", Integer.class, 8765);
        invokeSetter(stale, "setStatus", String.class, "online");
        invokeSetter(stale, "setLastHeartbeatTime", LocalDateTime.class, LocalDateTime.now().minusSeconds(16));
        handler.current = stale;

        Object service = newTeacherHostService(handler.proxy(), 15);

        assertNull(invokeObject(service, "currentHost", new Class<?>[]{Long.class}, 12L));
        assertEquals("offline", invokeObject(handler.current, "getStatus"));
    }

    /**
     * new教师主机Entity的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newTeacherHostEntity() throws Exception {
        return Class.forName("org.example.backend.entity.TeacherHostEntity").getConstructor().newInstance();
    }

    /**
     * new教师主机服务的处理逻辑。
     * @param mapper 数据映射器参数。
     * @param offlineSeconds offlineSeconds参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newTeacherHostService(Object mapper, int offlineSeconds) throws Exception {
        Class<?> mapperClass = Class.forName("org.example.backend.mapper.TeacherHostMapper");
        Class<?> serviceClass = Class.forName("org.example.backend.service.impl.TeacherHostServiceImpl");
        Constructor<?> constructor = serviceClass.getDeclaredConstructor(mapperClass, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(mapper, offlineSeconds);
    }

    /**
     * newFake教师主机数据映射器的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newFakeTeacherHostMapper() throws Exception {
        return new FakeTeacherHostMapper().proxy();
    }

    private static class FakeTeacherHostMapper implements InvocationHandler {
        private Object current;

        /**
         * 代理的处理逻辑。
         * @return Object 类型的处理结果。
         * @throws Exception 当前调用无法继续完成时抛出。
         */
        Object proxy() throws Exception {
            Class<?> mapperClass = Class.forName("org.example.backend.mapper.TeacherHostMapper");
            return Proxy.newProxyInstance(mapperClass.getClassLoader(), new Class<?>[]{mapperClass}, this);
        }

        /**
         * 调用当前操作。
         * @param proxy 代理参数。
         * @param method 方法参数。
         * @param args 参数参数。
         * @return Object 类型的处理结果。
         * @throws Throwable 当前调用无法继续完成时抛出。
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "upsertHeartbeat" -> {
                    current = args[0];
                    yield 1;
                }
                case "selectCurrentByLabId" -> {
                    Long labId = (Long) args[0];
                    yield current != null && labId.equals(invokeObject(current, "getLabId")) ? current : null;
                }
                case "markOffline" -> {
                    if (current != null) {
                        invokeSetter(current, "setStatus", String.class, "offline");
                    }
                    yield 1;
                }
                default -> null;
            };
        }
    }

    /**
     * 调用运行时。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @param parameterTypes parameter类型集合参数。
     */
    private static void invokeRuntime(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.invoke(target, args);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error actualError) {
                throw actualError;
            }
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    /**
     * 调用Object。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @param parameterTypes parameter类型集合参数。
     * @return Object 类型的处理结果。
     */
    private static Object invokeObject(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    /**
     * 调用Object。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @return Object 类型的处理结果。
     */
    private static Object invokeObject(Object target, String methodName) {
        return invokeObject(target, methodName, new Class<?>[0]);
    }

    /**
     * 调用Setter。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @param parameterType parameter类型参数。
     * @param value 值参数。
     */
    private static void invokeSetter(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
