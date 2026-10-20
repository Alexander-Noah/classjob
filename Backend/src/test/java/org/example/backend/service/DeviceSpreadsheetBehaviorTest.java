package org.example.backend.service;
import org.example.backend.entity.DevicesEntity;
import org.example.backend.mapper.*;
import org.example.backend.service.impl.DevicesServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceSpreadsheetBehaviorTest {
    @Test void csvImportSupportsQuotedMultilineEscapedNamesCrLfAndInvalidQuantities(){String csv="设备名称,数量,备注\r\n\"电脑,\"\"教学\"\"\",bad,\"第一行\n第二行\"\r\n\r\n路由器,,末行";assertEquals(2,service.importDevices(new MockMultipartFile("file","assets.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8))).getCount());var capture=ArgumentCaptor.forClass(DevicesEntity.class);verify(mapper,times(2)).InserterDevices(capture.capture());assertEquals("电脑,\"教学\"",capture.getAllValues().get(0).getDeviceName());assertEquals("第一行\n第二行",capture.getAllValues().get(0).getRemark());assertEquals(1,capture.getAllValues().get(0).getQuantity());assertEquals(1,capture.getAllValues().get(1).getQuantity());}
    @Test void fileReadFailureIsReportedBeforeAnyAssetIsInserted() throws Exception{var file=mock(org.springframework.web.multipart.MultipartFile.class);when(file.getOriginalFilename()).thenReturn("assets.csv");when(file.getSize()).thenReturn(10L);when(file.getInputStream()).thenThrow(new IOException("read denied"));assertThrows(IllegalArgumentException.class,()->service.importDevices(file));verify(mapper,never()).InserterDevices(any());}
    final DevicesMapper mapper=mock(DevicesMapper.class);
    final DevicesService service=new DevicesServiceImpl(mapper,mock(DeviceEventMapper.class),mock(LabMapper.class),mock(BusinessLoopService.class));
    byte[] zip(Map<String,String> files) throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("xl/"));zip.closeEntry();for(var file:files.entrySet()){zip.putNextEntry(new ZipEntry(file.getKey()));zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}return bytes.toByteArray();}
    String sheet(String content){return "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+content+"</sheetData><mergeCells><mergeCell ref=\"C2:C3\"/><mergeCell ref=\"invalid\"/></mergeCells></worksheet>";}
    String inline(String ref,String text){return "<c r=\""+ref+"\" t=\"inlineStr\"><is><t>"+text+"</t></is></c>";}
    @Test void xlsxImportResolvesSharedStringsInlineCellsMergedLabsAndNumericQuantities() throws Exception{String xml=sheet("<row>"+"<c r=\"A1\" t=\"s\"><v>0</v></c>"+inline("B1","数量")+inline("C1","实验室名称")+inline("D1","备注")+"</row><row>"+inline("A2","教学电脑")+"<c r=\"B2\"><v>2</v></c>"+inline("C2","A101")+"<c r=\"D2\" t=\"b\"><v>1</v></c></row><row>"+inline("A3","投影仪")+"<c r=\"B3\"><v>1</v></c><c r=\"D3\" t=\"b\"><v>0</v></c></row>");byte[] bytes=zip(Map.of("xl/sharedStrings.xml","<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><si><t>设备名称</t></si></sst>","xl/worksheets/sheet1.xml",xml));var result=service.importDevices(new MockMultipartFile("file","devices.xlsx","application/octet-stream",bytes));assertEquals(2,result.getCount());var devices=ArgumentCaptor.forClass(DevicesEntity.class);verify(mapper,times(2)).InserterDevices(devices.capture());assertEquals(List.of("教学电脑","投影仪"),devices.getAllValues().stream().map(DevicesEntity::getDeviceName).toList());assertEquals(2,devices.getAllValues().get(0).getQuantity());assertEquals("A101",devices.getAllValues().get(1).getLabName());assertEquals("true",devices.getAllValues().get(0).getRemark());assertEquals("false",devices.getAllValues().get(1).getRemark());}
    @Test void missingSheetMalformedXmlAndDoctypeCannotBeImported() throws Exception{for(Map<String,String> files:List.of(Map.of("other.xml","<x/>"),Map.of("xl/worksheets/sheet1.xml","bad"),Map.of("xl/worksheets/sheet1.xml","<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///not-used'>]><worksheet>&x;</worksheet>"))){byte[] bytes=zip(files);assertThrows(IllegalArgumentException.class,()->service.importDevices(new MockMultipartFile("file","devices.xlsx","application/octet-stream",bytes)));}verify(mapper,never()).InserterDevices(any());}
    @Test void sparseAndInvalidCellReferencesDoNotCreatePhantomAssets() throws Exception{String xml=sheet("<row>"+inline("A1","设备名称")+inline("AA1","备注")+"</row><row>"+inline("A2","路由器")+inline("AA2","远列备注")+inline("bad","忽略")+"<c r=\"B2\" t=\"s\"><v>99</v></c></row>");byte[] bytes=zip(Map.of("xl/worksheets/sheet1.xml",xml));assertEquals(1,service.importDevices(new MockMultipartFile("file","devices.xlsx","application/octet-stream",bytes)).getCount());verify(mapper).InserterDevices(argThat(device->"路由器".equals(device.getDeviceName())&&"远列备注".equals(device.getRemark())));}
    @Test void emptyUploadsAndHeaderOnlyDocumentsCannotInsertPhantomDevices() {
        assertThrows(IllegalArgumentException.class,()->service.importDevices(null));
        assertThrows(IllegalArgumentException.class,()->service.importDevices(new MockMultipartFile("file","assets.csv","text/csv",new byte[0])));
        for(String csv:List.of("设备名称,数量\r\n", "设备名称,数量\r,1\r路由器,2\r", "设备名称,数量\n路由器\n")) {
            int expected=csv.contains("路由器")?1:0;
            assertEquals(expected,service.importDevices(new MockMultipartFile("file","assets.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8))).getCount());
        }
        verify(mapper,times(2)).InserterDevices(any());
    }
    @Test void xlsxIgnoresInvalidCoordinatesAndOutOfBoundsSharedStrings() throws Exception {
        String xml=sheet("<row>"+inline("A1","设备名称")+inline("B1","备注")+"</row><row>"+inline("A2","路由器")+"<c r=\"B2\" t=\"s\"><v>-1</v></c><c t=\"s\"/><c r=\"A0\"/><c r=\"03\"/><c r=\"C2\" t=\"s\"/></row>");
        assertEquals(1,service.importDevices(new MockMultipartFile("file","assets.xlsx","application/octet-stream",zip(Map.of("xl/worksheets/sheet1.xml",xml)))).getCount());
        verify(mapper).InserterDevices(argThat(device->"路由器".equals(device.getDeviceName())&&"".equals(device.getRemark())));
    }
}
