/*
 * 设备管理回归测试；测试夹具与生产配置分离。
 * 覆盖场景：importRejectsUnsafeFilenameAndOversizedDeclaredFile、importRejectsXlsxArchivesWithTooManyOrOversizedEntries、quotedCsvFieldsRemainNormalizedAsExistingAssetRows、chineseDeviceListExpandsIntoTheExistingNormalizedRows。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.DevicesEntity;
import org.example.backend.mapper.DeviceEventMapper;
import org.example.backend.mapper.DevicesMapper;
import org.example.backend.mapper.LabMapper;
import org.example.backend.service.BusinessLoopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceImportSecurityTest {
    @Mock
    private DevicesMapper devicesMapper;

    @Mock
    private DeviceEventMapper eventMapper;

    @Mock
    private LabMapper labMapper;

    @Mock
    private BusinessLoopService businessLoopService;

    /**
     * 导入RejectsUnsafeFilenameAndOversizedDeclared文件。
     */
    @Test
    void importRejectsUnsafeFilenameAndOversizedDeclaredFile() {
        byte[] bytes = "deviceName\n台式电脑".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, /** 处理 assertThrows 调用中的回调逻辑。 */ () -> service().importDevices(file("assets.exe", 10, bytes)));
        assertThrows(IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service().importDevices(file("assets.xlsx", 10_485_761, bytes)));
    }

    /**
     * 导入RejectsXlsx归档WithTooManyOrOversized条目集合。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    @Test
    void importRejectsXlsxArchivesWithTooManyOrOversizedEntries() throws IOException {
        assertThrows(IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service().importDevices(file("assets.xlsx", 10, zipWithEntries(129))));
        assertThrows(IllegalArgumentException.class,
                /**
                 * 处理 assertThrows 调用中的回调逻辑。
                 */
                () -> service().importDevices(file("assets.xlsx", 10, zipWithEntryLargerThan5MiB())));
    }

    /**
     * quotedCSVFieldsRemain规范化结果AsExisting资产行集合的处理逻辑。
     */
    @Test
    void quotedCsvFieldsRemainNormalizedAsExistingAssetRows() {
        byte[] csv = ("assetNo,deviceName,category,labName,roomNo,quantity,unit,status,specification,standardRequirement,remark\n"
                + "A-1,\"演示,设备\",其他,实验室A,A101,2,台,正常,\"配置,参数\",标准,\"备注,含逗号\"\n")
                .getBytes(StandardCharsets.UTF_8);

        service().importDevices(file("assets.csv", csv.length, csv));

        ArgumentCaptor<DevicesEntity> devices = ArgumentCaptor.forClass(DevicesEntity.class);
        verify(devicesMapper).InserterDevices(devices.capture());
        DevicesEntity imported = devices.getValue();
        assertEquals("A-1", imported.getDeviceCode());
        assertEquals("演示,设备", imported.getDeviceName());
        assertEquals("配置,参数", imported.getSpecs());
        assertEquals("备注,含逗号", imported.getRemark());
    }

    /**
     * chinese设备列表ExpandsIntoTheExisting规范化结果行集合的处理逻辑。
     */
    @Test
    void chineseDeviceListExpandsIntoTheExistingNormalizedRows() {
        byte[] csv = ("主要设备名称,设备名称,主要功能和技术要求\n"
                + "\"台式电脑：联想启天M410 1台 交换机：TP-LINK 24口 1台\",联想启天M410,\"CPU i5，内存16G\"\n")
                .getBytes(StandardCharsets.UTF_8);

        service().importDevices(file("assets.csv", csv.length, csv));

        ArgumentCaptor<DevicesEntity> devices = ArgumentCaptor.forClass(DevicesEntity.class);
        verify(devicesMapper, times(2)).InserterDevices(devices.capture());
        List<DevicesEntity> imported = devices.getAllValues();
        assertEquals("联想启天M410", imported.get(0).getDeviceName());
        assertEquals("计算终端", imported.get(0).getCategory());
        assertEquals("CPU i5，内存16G", imported.get(0).getSpecs());
        assertEquals("TP-LINK 24口", imported.get(1).getDeviceName());
        assertEquals("网络设备", imported.get(1).getCategory());
        assertEquals("", imported.get(1).getSpecs());
    }

    /**
     * 服务的处理逻辑。
     * @return DevicesServiceImpl 类型的处理结果。
     */
    private DevicesServiceImpl service() {
        return new DevicesServiceImpl(devicesMapper, eventMapper, labMapper, businessLoopService);
    }

    /**
     * 文件的处理逻辑。
     * @param filename filename参数。
     * @param declaredSize declared大小参数。
     * @param content 内容参数。
     * @return MultipartFile 类型的处理结果。
     */
    private MultipartFile file(String filename, long declaredSize, byte[] content) {
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
             * @param destination destination参数。
             * @throws IOException 当前调用无法继续完成时抛出。
             */
            @Override
            public void transferTo(File destination) throws IOException {
                throw new IOException("not implemented");
            }
        };
    }

    /**
     * zipWith条目集合的处理逻辑。
     * @param count 数量参数。
     * @return byte[] 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private byte[] zipWithEntries(int count) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int index = 0; index < count; index++) {
                zip.putNextEntry(new ZipEntry("xl/entry-" + index + ".xml"));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    /**
     * zipWith条目LargerThan5MiB的处理逻辑。
     * @return byte[] 类型的处理结果。
     * @throws IOException 当前调用无法继续完成时抛出。
     */
    private byte[] zipWithEntryLargerThan5MiB() throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zip.write(new byte[5 * 1024 * 1024 + 1]);
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
