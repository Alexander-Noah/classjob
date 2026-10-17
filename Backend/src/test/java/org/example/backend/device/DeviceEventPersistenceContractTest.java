/*
 * 设备事件回归测试；测试夹具与生产配置分离。
 * 覆盖场景：deviceLifecycleUsesOneEventTableWithoutRuntimeDdl。
 */
package org.example.backend.device;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceEventPersistenceContractTest {
    /**
     * 设备LifecycleUsesOne事件TableWithout运行时Ddl的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void deviceLifecycleUsesOneEventTableWithoutRuntimeDdl() throws Exception {
        String mapper = Files.readString(resolveModulePath("src", "main", "resources", "mapper", "DeviceEventMapper.xml"));
        String service = Files.readString(resolveModulePath("src", "main", "java", "org", "example", "backend", "service", "impl", "DevicesServiceImpl.java"));
        String inventoryInsert = between(mapper, "<insert id=\"insertInventoryEvent\"", "</insert>");
        String transferInsert = between(mapper, "<insert id=\"insertTransferEvent\"", "</insert>");
        assertAll(
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("insert into device_event")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("event_type = 'inventory'")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(mapper.contains("event_type = 'transfer'")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(inventoryInsert.contains("useGeneratedKeys=\"true\" keyProperty=\"id\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(transferInsert.contains("useGeneratedKeys=\"true\" keyProperty=\"id\"")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("createTableIfNotExists")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("DeviceInventoryRecordMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertFalse(service.contains("DeviceTransferRecordMapper")),
                /**
                 * 处理 assertAll 调用中的回调逻辑。
                 */
                () -> assertTrue(service.contains("DeviceEventMapper"))
        );
    }

    /**
     * 解析Module路径。
     * @return Path 类型的处理结果。
     */
    private Path resolveModulePath(String... parts) {
        Path modulePath = Path.of(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            modulePath = modulePath.resolve(parts[i]);
        }
        return modulePath;
    }

    /**
     * between的处理逻辑。
     * @param value 值参数。
     * @param startMarker 开始Marker参数。
     * @param endMarker 结束Marker参数。
     * @return String 类型的处理结果。
     */
    private String between(String value, String startMarker, String endMarker) {
        int start = value.indexOf(startMarker);
        assertTrue(start >= 0, startMarker + " should exist");
        int end = value.indexOf(endMarker, start);
        assertTrue(end > start, endMarker + " should exist after " + startMarker);
        return value.substring(start, end + endMarker.length());
    }
}
