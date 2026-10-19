package org.example.backend.controller;
import org.example.backend.service.DevicesService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeviceExportEndpointBehaviorTest {
    @Test void devicePageImportAndCsvExportPreserveFiltersAndDownloadMetadata() throws Exception{DevicesService service=mock(DevicesService.class);var mvc=MockMvcBuilders.standaloneSetup(new DevicesController(service)).build();byte[] csv="assetNo,deviceName\nPC1,电脑".getBytes(java.nio.charset.StandardCharsets.UTF_8);when(service.exportDevices(any())).thenReturn(csv);when(service.importDevices(any())).thenReturn(new DevicesService.DeviceImportResult(true,"导入完成",1));mvc.perform(get("/devices/page").param("pageNum","2").param("pageSize","20").param("labId","7").param("labName","机房").param("category","电脑").param("status","正常")).andExpect(status().isOk());verify(service).pageDevices(argThat(q->q.getPageNum()==2&&q.getPageSize()==20&&q.getLabId()==7&&"机房".equals(q.getLabName())&&"电脑".equals(q.getCategory())&&"正常".equals(q.getStatus())));mvc.perform(get("/devices/stats")).andExpect(status().isOk());mvc.perform(get("/devices/export")).andExpect(content().bytes(csv)).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("filename*=UTF-8''")));mvc.perform(multipart("/devices/import").file(new MockMultipartFile("file","assets.csv","text/csv",csv))).andExpect(jsonPath("$.data.success").value(true)).andExpect(jsonPath("$.data.count").value(1));mvc.perform(get("/devices/7")).andExpect(status().isOk());mvc.perform(post("/devices/reset")).andExpect(status().isOk());mvc.perform(put("/devices/batch").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());verify(service).getDeviceStats();verify(service).getDevicesById(7L);verify(service).getDevices();verify(service).updateDevices(List.of());}
}
