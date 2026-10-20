package org.example.backend.service;
import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import org.example.backend.config.AliyunOssProperties;
import org.example.backend.service.impl.AliyunOssServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssBehaviorTest {
    AliyunOssProperties config(){AliyunOssProperties config=new AliyunOssProperties();config.setEnabled(true);config.setEndpoint("https://oss.test");config.setBucket("lab-test");config.setAccessKeyId("test-id");config.setAccessKeySecret("test-secret");config.setAvatarDir("/avatars/");return config;}
    @Test void uploadUsesValidatedMetadataGeneratesSafeObjectKeyAndAlwaysClosesClient(){AliyunOssProperties config=config();config.setPublicBaseUrl("https://cdn.test///");OSS client=mock(OSS.class);try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))){var service=new AliyunOssServiceImpl(config);byte[] png={(byte)0x89,'P','N','G',13,10,26,10};Map<String,String> result=service.uploadAvatar(new MockMultipartFile("file","profile.PNG","image/png",png));assertTrue(result.get("objectKey").matches("avatars/\\d{4}/\\d{2}/\\d{2}/[\\da-f-]+\\.png"));assertEquals("https://cdn.test/"+result.get("objectKey"),result.get("url"));assertEquals(result.get("url"),result.get("avatarUrl"));verify(client).putObject(eq("lab-test"),eq(result.get("objectKey")),any(InputStream.class),argThat(metadata->metadata.getContentLength()==8&&metadata.getContentType().equals("image/png")));verify(client).shutdown();}}
    @Test void supportedImageSignaturesAndMissingExtensionsUseMatchingDefaults(){AliyunOssProperties config=config();OSS client=mock(OSS.class);try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))){var service=new AliyunOssServiceImpl(config);for(Object[] fixture:new Object[][]{{"image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255},".jpg"},{"image/gif","GIF89a".getBytes(),".gif"},{"image/webp","RIFF1234WEBP".getBytes(),".webp"}}){Map<String,String> result=service.uploadAvatar(new MockMultipartFile("file","avatar",fixture[0].toString(),(byte[])fixture[1]));assertTrue(result.get("objectKey").endsWith(fixture[2].toString()));assertTrue(result.get("url").startsWith("https://lab-test.oss.test/"));}verify(client,times(3)).shutdown();}}
    @Test void downloadNormalizesOnlyAvatarPrefixAndClosesContentAndClient() throws Exception{OSS client=mock(OSS.class);OSSObject object=new OSSObject();ObjectMetadata metadata=new ObjectMetadata();metadata.setContentType("image/png");object.setObjectMetadata(metadata);InputStream content=spy(new ByteArrayInputStream(new byte[]{1,2,3}));object.setObjectContent(content);when(client.getObject("lab-test","avatars/7.png")).thenReturn(object);try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))){var service=new AliyunOssServiceImpl(config());var result=service.downloadAvatar("/avatars/7.png/");assertArrayEquals(new byte[]{1,2,3},result.content());assertEquals("image/png",result.contentType());verify(content).close();verify(client).shutdown();for(String key:Arrays.asList(null,"","other/7.png","avatars/../7.png"))assertThrows(IllegalArgumentException.class,()->service.downloadAvatar(key));}}
    @Test void remoteReadFailuresRemainUserFacingAndAlwaysShutdownClient(){OSS client=mock(OSS.class);when(client.getObject(anyString(),anyString())).thenThrow(new OSSException("denied"));try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))){assertThrows(IllegalArgumentException.class,()->new AliyunOssServiceImpl(config()).downloadAvatar("avatars/missing.png"));verify(client).shutdown();}}
    @Test void configurationAndContentFailuresCannotCreateOssClients(){AliyunOssProperties config=config();config.setEnabled(false);assertThrows(IllegalStateException.class,()->new AliyunOssServiceImpl(config).uploadAvatar(null));config.setEnabled(true);config.setEndpoint("");assertThrows(IllegalStateException.class,()->new AliyunOssServiceImpl(config).uploadAvatar(null));config.setEndpoint("oss.test");var service=new AliyunOssServiceImpl(config);assertThrows(IllegalArgumentException.class,()->service.uploadAvatar(null));assertThrows(IllegalArgumentException.class,()->service.uploadAvatar(new MockMultipartFile("file","bad.jpg","image/png",new byte[]{1,2,3})));assertThrows(IllegalArgumentException.class,()->service.uploadAvatar(new MockMultipartFile("file","bad.gif","image/gif",new byte[]{'G','I'})));}
    @Test void missingConfigurationAndUnsupportedOrTruncatedImageFilesNeverContactStorage() {
        for(String key:List.of("bucket","id","secret")) {
            var configuration=config();if(key.equals("bucket"))configuration.setBucket("");else if(key.equals("id"))configuration.setAccessKeyId("");else configuration.setAccessKeySecret("");
            assertThrows(IllegalStateException.class,()->new AliyunOssServiceImpl(configuration).uploadAvatar(null));
        }
        var service=new AliyunOssServiceImpl(config());
        for(Object[] file:new Object[][]{{null,new byte[]{1}},{"text/html",new byte[]{1}},{"image/png",new byte[]{(byte)137}},{"image/webp","RIFF1234BAD!".getBytes()},{"image/webp","BAD!1234WEBP".getBytes()}}) {
            assertThrows(IllegalArgumentException.class,()->service.uploadAvatar(new MockMultipartFile("file","avatar",(String)file[0],(byte[])file[1])));
        }
        assertThrows(IllegalArgumentException.class,()->service.uploadAvatar(new MockMultipartFile("file","avatar.png","image/png",new byte[0])));
    }
    @Test void unreadableUploadStreamsAreReportedAndClientIsAlwaysReleased() throws Exception {
        var file=mock(org.springframework.web.multipart.MultipartFile.class);when(file.getSize()).thenReturn(8L);when(file.getOriginalFilename()).thenReturn(null);when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new IOException("broken stream"));
        assertEquals("头像文件读取失败",assertThrows(IllegalArgumentException.class,()->new AliyunOssServiceImpl(config()).uploadAvatar(file)).getMessage());
        byte[] png={(byte)137,'P','N','G',13,10,26,10};doReturn(new ByteArrayInputStream(png)).doThrow(new IOException("second read failed")).when(file).getInputStream();
        OSS client=mock(OSS.class);try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))) {
            assertThrows(IllegalArgumentException.class,()->new AliyunOssServiceImpl(config()).uploadAvatar(file));verify(client).shutdown();
        }
    }
    @Test void absentDownloadMetadataFallsBackToBinaryAndReadFailuresReleaseClient() throws Exception {
        OSS client=mock(OSS.class);OSSObject object=mock(OSSObject.class);when(client.getObject(anyString(),anyString())).thenReturn(object);
        when(object.getObjectContent()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))) {
            var service=new AliyunOssServiceImpl(config());assertEquals("application/octet-stream",service.downloadAvatar("avatars/a.png").contentType());
            InputStream broken=mock(InputStream.class);when(broken.readAllBytes()).thenThrow(new IOException("read failed"));when(object.getObjectContent()).thenReturn(broken);
            assertThrows(IllegalArgumentException.class,()->service.downloadAvatar("avatars/a.png"));verify(client,times(2)).shutdown();verify(broken).close();
        }
    }
    @Test void legacyGifAndBlankAvatarDirectoryUseSafeGeneratedDefaults() {
        var configuration=config();configuration.setAvatarDir(" ");OSS client=mock(OSS.class);
        try(var builders=mockConstruction(OSSClientBuilder.class,(builder,context)->when(builder.build(anyString(),anyString(),anyString())).thenReturn(client))) {
            var result=new AliyunOssServiceImpl(configuration).uploadAvatar(new MockMultipartFile("file","profile.","image/gif","GIF87a".getBytes()));
            assertTrue(result.get("objectKey").startsWith("avatars/"));assertTrue(result.get("objectKey").endsWith(".gif"));verify(client).shutdown();
        }
    }
}
