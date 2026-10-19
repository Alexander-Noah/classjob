package org.example.backend.controller;

import org.example.backend.config.SecurityHeadersFilter;
import org.example.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActiveContentResponseTest {
    @Test void avatarCannotServeStoredHtmlOrSvgAsAnActiveDocument() throws Exception {
        var service=mock(AliyunOssService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new FileUploadController(service))
                .addFilters(new SecurityHeadersFilter(true)).build();
        for(String type: java.util.List.of("text/html","image/svg+xml","application/javascript")) {
            when(service.downloadAvatar("avatars/payload")).thenReturn(new AliyunOssService.AvatarObject("<script>alert(1)</script>".getBytes(),type));
            mvc.perform(get("/files/avatar-proxy").param("objectKey","avatars/payload"))
                    .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(header().string("X-Content-Type-Options","nosniff"))
                    .andExpect(header().string("Content-Disposition","attachment; filename=\"avatar.bin\""))
                    .andExpect(header().string("Content-Security-Policy","default-src 'none'; sandbox"));
        }
    }
    @Test void cameraCannotRelayDeviceProvidedHtmlAsSameOriginPage() throws Exception {
        var hardware=mock(IotHardwareService.class);
        var devices=mock(IotDeviceService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new IotHardwareController(hardware,devices))
                .addFilters(new SecurityHeadersFilter(true)).build();
        when(hardware.getCameraSnapshot("CAM1")).thenReturn(new IotHardwareService.CameraSnapshot("<svg onload=alert(1)>".getBytes(),"image/svg+xml"));
        mvc.perform(get("/iot/cameras/CAM1/snapshot"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"snapshot.bin\""))
                .andExpect(header().string("Content-Security-Policy","default-src 'none'; sandbox"));
    }
}
