/*
 * 设备管理回归测试；测试夹具与生产配置分离。
 * 覆盖场景：normalizeDeviceBindsLabByLabId、normalizeDeviceBindsLabByLocationWhenLabIdMissing、importRowAppliesSpecificationOnlyToComputerOrMatchingDeviceName、importRowAllowsSpecificationForNonComputerDeviceWhenDeviceNameMatchesColumnJ 等。
 */
package org.example.backend.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevicesServiceImplImportTest {
    /**
     * 归一化设备Binds实验室By实验室标识。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void normalizeDeviceBindsLabByLabId() throws Exception {
        Object service = newDevicesServiceWithLabs(List.of(newLab(7, "软件部署实训室", "2-412")));
        Object device = newDevice();
        invokeSetter(device, "setDeviceName", String.class, "教师机");
        invokeSetter(device, "setLabId", Long.class, 7L);

        invokeNormalizeDevice(service, device);

        assertEquals(7L, invokeObject(device, "getLabId"));
        assertEquals("软件部署实训室", invokeString(device, "getLabName"));
        assertEquals("2-412", invokeString(device, "getLocation"));
    }

    /**
     * 归一化设备Binds实验室ByLocationWhen实验室标识Missing。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void normalizeDeviceBindsLabByLocationWhenLabIdMissing() throws Exception {
        Object service = newDevicesServiceWithLabs(List.of(newLab(8, "WEB后端开发实训室", "2-209")));
        Object device = newDevice();
        invokeSetter(device, "setDeviceName", String.class, "2-209 交换机");
        invokeSetter(device, "setLocation", String.class, "2-209");

        invokeNormalizeDevice(service, device);

        assertEquals(8L, invokeObject(device, "getLabId"));
        assertEquals("WEB后端开发实训室", invokeString(device, "getLabName"));
        assertEquals("2-209", invokeString(device, "getLocation"));
    }

    /**
     * 导入行AppliesSpecificationOnly目标ComputerOrMatching设备名称。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void importRowAppliesSpecificationOnlyToComputerOrMatchingDeviceName() throws Exception {
        Object service = newDevicesService();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("实验室名称", "智慧实验室");
        row.put("地点", "A101");
        row.put("主要设备名称", String.join(" ",
                "台式电脑：联想启天M410 1台",
                "交换机：TP-LINK 24口 1台",
                "交换机：TP-LINK 48口 1台",
                "投影仪：卡西欧FC330 1台",
                "空调：KFR-72LW/DY-X(E2) 1台"));
        row.put("设备名称", "联想启天M410");
        row.put("主要功能和技术要求", "CPU i5，内存16G，硬盘1T");

        List<Object> devices = invokeMapImportRow(service, row);
        Map<String, Object> devicesByName = devices.stream()
                .collect(Collectors.toMap(/** 处理 toMap 调用中的回调逻辑。 入参：device。 */ device -> invokeString(device, "getDeviceName"), /** 处理 toMap 调用中的回调逻辑。 入参：device。 */ device -> device));

        assertEquals("CPU i5，内存16G，硬盘1T", invokeString(devicesByName.get("联想启天M410"), "getSpecs"));
        assertEquals("", invokeString(devicesByName.get("TP-LINK 24口"), "getSpecs"));
        assertEquals("", invokeString(devicesByName.get("TP-LINK 48口"), "getSpecs"));
        assertEquals("", invokeString(devicesByName.get("卡西欧FC330"), "getSpecs"));
        assertEquals("", invokeString(devicesByName.get("KFR-72LW/DY-X(E2)"), "getSpecs"));

        assertEquals("计算终端", invokeString(devicesByName.get("联想启天M410"), "getCategory"));
        assertEquals("网络设备", invokeString(devicesByName.get("TP-LINK 24口"), "getCategory"));
        assertEquals("网络设备", invokeString(devicesByName.get("TP-LINK 48口"), "getCategory"));
        assertEquals("显示设备", invokeString(devicesByName.get("卡西欧FC330"), "getCategory"));
        assertEquals("环境设备", invokeString(devicesByName.get("KFR-72LW/DY-X(E2)"), "getCategory"));
    }

    /**
     * 导入行AllowsSpecificationFor非Computer设备When设备名称MatchesColumnJ。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void importRowAllowsSpecificationForNonComputerDeviceWhenDeviceNameMatchesColumnJ() throws Exception {
        Object service = newDevicesService();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("主要设备名称", "投影仪：卡西欧FC330 1台 空调：KFR-72LW/DY-X(E2) 1台");
        row.put("设备名称", "卡西欧FC330");
        row.put("主要功能和技术要求", "3300流明，短焦投影");

        List<Object> devices = invokeMapImportRow(service, row);
        Map<String, Object> devicesByName = devices.stream()
                .collect(Collectors.toMap(/** 处理 toMap 调用中的回调逻辑。 入参：device。 */ device -> invokeString(device, "getDeviceName"), /** 处理 toMap 调用中的回调逻辑。 入参：device。 */ device -> device));

        assertEquals("3300流明，短焦投影", invokeString(devicesByName.get("卡西欧FC330"), "getSpecs"));
        assertEquals("", invokeString(devicesByName.get("KFR-72LW/DY-X(E2)"), "getSpecs"));
    }

    /**
     * 调用映射Import行。
     * @param service 服务参数。
     * @param row 行参数。
     * @return 当前操作得到的记录集合。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @SuppressWarnings("unchecked")
    private List<Object> invokeMapImportRow(Object service, Map<String, Object> row) throws Exception {
        Method method = service.getClass().getDeclaredMethod("mapImportRow", Map.class);
        method.setAccessible(true);
        return (List<Object>) method.invoke(service, row);
    }

    /**
     * 导入设备Rejects文件集合LargerThan上限。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void importDevicesRejectsFilesLargerThanLimit() throws Exception {
        Object service = newDevicesService();
        MultipartFile file = multipartFile("devices.csv", 10L * 1024 * 1024 + 1, "name\ncomputer".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> invokeImportDevices(service, file));

        assertTrue(error.getMessage().contains("10MB"));
    }

    /**
     * 导入设备RejectsUnsupportedExtension。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void importDevicesRejectsUnsupportedExtension() throws Exception {
        Object service = newDevicesService();
        MultipartFile file = multipartFile("devices.txt", 12, "name\ncomputer".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> invokeImportDevices(service, file));

        assertTrue(error.getMessage().contains("CSV") || error.getMessage().contains("xlsx"));
    }

    /**
     * 读取Zip条目集合RejectsOversized条目。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void readZipEntriesRejectsOversizedEntry() throws Exception {
        Object service = newDevicesService();
        Method method = service.getClass().getDeclaredMethod("readZipEntries", InputStream.class);
        method.setAccessible(true);
        byte[] zipBytes = zipWithEntry("xl/sharedStrings.xml", new byte[5 * 1024 * 1024 + 1]);

        Exception error = assertThrows(Exception.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> method.invoke(service, new ByteArrayInputStream(zipBytes)));

        Throwable cause = error.getCause() == null ? error : error.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertTrue(cause.getMessage().contains("too large"));
    }

    /**
     * 调用Import设备。
     * @param service 服务参数。
     * @param file 文件参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object invokeImportDevices(Object service, MultipartFile file) throws Exception {
        try {
            Method method = service.getClass().getMethod("importDevices", MultipartFile.class);
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
     * new设备服务的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newDevicesService() throws Exception {
        return newDevicesServiceWithLabs(List.of());
    }

    /**
     * multipart文件的处理逻辑。
     * @param filename filename参数。
     * @param declaredSize declared大小参数。
     * @param content 内容参数。
     * @return MultipartFile 类型的处理结果。
     */
    private MultipartFile multipartFile(String filename, long declaredSize, byte[] content) {
        return new MultipartFile() {
            /**
             * 获取名称。
             * @return String 类型的处理结果。
             */
            @Override
            public String getName() {
                return "file";
            }

            /**
             * 获取OriginalFilename。
             * @return String 类型的处理结果。
             */
            @Override
            public String getOriginalFilename() {
                return filename;
            }

            /**
             * 获取内容类型。
             * @return String 类型的处理结果。
             */
            @Override
            public String getContentType() {
                return "application/octet-stream";
            }

            /**
             * 判断是否为空值。
             * @return 当前判断的布尔结果。
             */
            @Override
            public boolean isEmpty() {
                return declaredSize == 0;
            }

            /**
             * 获取大小。
             * @return long 类型的处理结果。
             */
            @Override
            public long getSize() {
                return declaredSize;
            }

            /**
             * 获取Bytes。
             * @return byte[] 类型的处理结果。
             */
            @Override
            public byte[] getBytes() {
                return content;
            }

            /**
             * 获取输入Stream。
             * @return InputStream 类型的处理结果。
             */
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content);
            }

            /**
             * 调拨目标。
             * @param dest dest参数。
             * @throws IOException 当前调用无法继续完成时抛出。
             */
            @Override
            public void transferTo(File dest) throws IOException {
                throw new IOException("not implemented");
            }
        };
    }

    /**
     * zipWith条目的处理逻辑。
     * @param name 名称参数。
     * @param content 内容参数。
     * @return byte[] 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private byte[] zipWithEntry(String name, byte[] content) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    /**
     * new设备服务With实验室的处理逻辑。
     * @param labs 实验室参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newDevicesServiceWithLabs(List<Object> labs) throws Exception {
        Class<?> serviceClass = Class.forName("org.example.backend.service.impl.DevicesServiceImpl");
        Constructor<?> constructor = serviceClass.getConstructors()[0];
        Object[] args = new Object[constructor.getParameterCount()];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if ("org.example.backend.mapper.LabMapper".equals(parameterTypes[index].getName())) {
                args[index] = fakeLabMapper(parameterTypes[index], labs);
            }
        }
        return constructor.newInstance(args);
    }

    /**
     * fake实验室数据映射器的处理逻辑。
     * @param mapperClass 数据映射器Class参数。
     * @param labs 实验室参数。
     * @return Object 类型的处理结果。
     */
    private Object fakeLabMapper(Class<?> mapperClass, List<Object> labs) {
        InvocationHandler handler = /** 处理当前调用传入的回调逻辑。 入参：proxy、method、args。 */ (proxy, method, args) -> {
            if ("getLabs".equals(method.getName())) {
                return labs;
            }
            if ("getLabById".equals(method.getName())) {
                Integer id = args == null || args.length == 0 ? null : (Integer) args[0];
                return labs.stream().filter(/** 判断当前条目是否符合筛选条件。 入参：lab。 */ lab -> invokeObject(lab, "getId").equals(id)).findFirst().orElse(null);
            }
            return null;
        };
        return Proxy.newProxyInstance(
                mapperClass.getClassLoader(),
                new Class<?>[]{mapperClass},
                handler
        );
    }

    /**
     * new实验室的处理逻辑。
     * @param id 标识参数。
     * @param labName 实验室名称参数。
     * @param roomNo roomNo参数。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newLab(int id, String labName, String roomNo) throws Exception {
        Class<?> labClass = Class.forName("org.example.backend.entity.LabEntity");
        Object lab = labClass.getConstructor().newInstance();
        invokeSetter(lab, "setId", Integer.class, id);
        invokeSetter(lab, "setLabName", String.class, labName);
        invokeSetter(lab, "setRoomNo", String.class, roomNo);
        return lab;
    }

    /**
     * new设备的处理逻辑。
     * @return Object 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private Object newDevice() throws Exception {
        Class<?> deviceClass = Class.forName("org.example.backend.entity.DevicesEntity");
        return deviceClass.getConstructor().newInstance();
    }

    /**
     * 调用Normalize设备。
     * @param service 服务参数。
     * @param device 设备参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void invokeNormalizeDevice(Object service, Object device) throws Exception {
        Method method = service.getClass().getDeclaredMethod("normalizeDevice", device.getClass());
        method.setAccessible(true);
        method.invoke(service, device);
    }

    /**
     * 调用Setter。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @param parameterType parameter类型参数。
     * @param value 值参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void invokeSetter(Object target, String methodName, Class<?> parameterType, Object value) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterType);
        method.invoke(target, value);
    }

    /**
     * 调用Object。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @return Object 类型的处理结果。
     */
    private Object invokeObject(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception error) {
            throw new AssertionError("Failed to invoke " + methodName, error);
        }
    }

    /**
     * 调用字符串。
     * @param target 目标参数。
     * @param methodName 方法名称参数。
     * @return String 类型的处理结果。
     */
    private String invokeString(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception error) {
            throw new AssertionError("Failed to invoke " + methodName, error);
        }
    }
}
