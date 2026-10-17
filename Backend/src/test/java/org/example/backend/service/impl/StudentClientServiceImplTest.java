/*
 * 学生客户端回归测试；测试夹具与生产配置分离。
 * 覆盖场景：onlineRequiresStudentLabId、listReturnsOnlySameLabStudents。
 */
package org.example.backend.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentClientServiceImplTest {
    /**
     * onlineRequires学生实验室标识的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void onlineRequiresStudentLabId() throws Exception {
        Object service = newStudentClientService(new FakeStudentClientMapper().proxy());
        Object request = newStudentClientEntity();
        invokeSetter(request, "setStudentDeviceId", String.class, "student-1");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> invokeRuntime(service, "online", new Class<?>[]{request.getClass()}, request)
        );

        assertEquals("labId must be positive", error.getMessage());
    }

    /**
     * 列出ReturnsOnlySame实验室学生。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void listReturnsOnlySameLabStudents() throws Exception {
        FakeStudentClientMapper handler = new FakeStudentClientMapper();
        Object sameLab = newStudentClientEntity();
        invokeSetter(sameLab, "setLabId", Long.class, 7L);
        invokeSetter(sameLab, "setStudentDeviceId", String.class, "student-7");
        handler.records.add(sameLab);
        Object otherLab = newStudentClientEntity();
        invokeSetter(otherLab, "setLabId", Long.class, 8L);
        invokeSetter(otherLab, "setStudentDeviceId", String.class, "student-8");
        handler.records.add(otherLab);

        Object service = newStudentClientService(handler.proxy());

        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) invokeObject(service, "listByLabId", new Class<?>[]{Long.class}, 7L);

        assertEquals(1, result.size());
        assertEquals("student-7", invokeObject(result.get(0), "getStudentDeviceId"));
    }

    /**
     * new学生客户端Entity的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newStudentClientEntity() throws Exception {
        return Class.forName("org.example.backend.entity.StudentClientEntity").getConstructor().newInstance();
    }

    /**
     * new学生客户端服务的处理逻辑。
     * @param mapper 数据映射器参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newStudentClientService(Object mapper) throws Exception {
        Class<?> mapperClass = Class.forName("org.example.backend.mapper.StudentClientMapper");
        Class<?> serviceClass = Class.forName("org.example.backend.service.impl.StudentClientServiceImpl");
        Constructor<?> constructor = serviceClass.getConstructor(mapperClass);
        return constructor.newInstance(mapper);
    }

    private static class FakeStudentClientMapper implements InvocationHandler {
        private final List<Object> records = new ArrayList<>();

        /**
         * 代理的处理逻辑。
         * @return Object 类型的处理结果。
         * @throws Exception 当前调用无法继续完成时抛出。
         */
        Object proxy() throws Exception {
            Class<?> mapperClass = Class.forName("org.example.backend.mapper.StudentClientMapper");
            return Proxy.newProxyInstance(mapperClass.getClassLoader(), new Class<?>[]{mapperClass}, this);
        }

        /**
         * 调用当前操作。
         * @param proxy 代理参数。
         * @param method 方法参数。
         * @param args 参数参数。
         * @return Object 类型的处理结果。
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "upsertOnline" -> {
                    records.add(args[0]);
                    yield 1;
                }
                case "markOffline" -> {
                    String studentDeviceId = (String) args[0];
                    Long labId = (Long) args[1];
                    records.stream()
                            .filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> studentDeviceId.equals(invokeObject(item, "getStudentDeviceId"))
                                    && labId.equals(invokeObject(item, "getLabId")))
                            .forEach(/** 处理遍历到的当前条目。 入参：item。 */ item -> invokeSetter(item, "setStatus", String.class, "offline"));
                    yield 1;
                }
                case "selectByLabId" -> {
                    Long labId = (Long) args[0];
                    yield records.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ item -> labId.equals(invokeObject(item, "getLabId"))).toList();
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
