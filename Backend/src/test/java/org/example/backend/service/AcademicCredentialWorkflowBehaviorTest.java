package org.example.backend.service;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.impl.AcademicCredentialServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicCredentialWorkflowBehaviorTest {
    @Test void encryptedCredentialCanBeAppliedInternallyAndMalformedCipherFailsClosed(){ClassTimetableMapper mapper=mock(ClassTimetableMapper.class);Map<String,Map<String,Object>> rows=new HashMap<>();when(mapper.getCrawlerCredential(anyString())).thenAnswer(c->rows.get(c.getArgument(0)));when(mapper.upsertCrawlerCredential(anyString(),anyString(),anyString())).thenAnswer(c->{rows.put(c.getArgument(0),new HashMap<>(Map.of("usernameCipher",c.getArgument(1),"passwordCipher",c.getArgument(2))));return 1;});AcademicCredentialServiceImpl service=new AcademicCredentialServiceImpl(mapper,"0123456789abcdef0123456789abcdef");AcademicScheduleImportRequest request=new AcademicScheduleImportRequest();request.setCredentialKey("class/one");request.setUsername("teacher");request.setPassword("test-password");assertTrue(service.saveCredential(request).getConfigured());assertFalse(rows.toString().contains("test-password"));request.setUsername(null);request.setPassword(null);service.applySavedCredential(request);assertEquals("teacher",request.getUsername());assertEquals("test-password",request.getPassword());service.deleteCredential("class/one");verify(mapper).deleteCrawlerCredential("class_one");rows.get("class_one").put("usernameCipher","bad");assertThrows(IllegalStateException.class,()->service.getCredentialView("class/one"));assertFalse(service.getCredentialView(null).getConfigured());assertThrows(IllegalArgumentException.class,()->service.applySavedCredential(null));}
    @Test void missingPartialCredentialsAndWeakSecretsAreRejected(){ClassTimetableMapper mapper=mock(ClassTimetableMapper.class);for(String secret:List.of("short","change-me-0123456789abcdef0123456789abcdef","dev-secret-0123456789abcdef0123456789abcdef"))assertThrows(IllegalStateException.class,()->new AcademicCredentialServiceImpl(mapper,secret));var service=new AcademicCredentialServiceImpl(mapper,"0123456789abcdef0123456789abcdef");var request=new AcademicScheduleImportRequest();assertThrows(IllegalArgumentException.class,()->service.saveCredential(request));when(mapper.getCrawlerCredential("default")).thenReturn(Map.of());assertFalse(service.getCredentialView("").getConfigured());assertThrows(IllegalArgumentException.class,()->service.applySavedCredential(request));when(mapper.getCrawlerCredential("default")).thenReturn(Map.of("usernameCipher","cipher"));assertThrows(IllegalArgumentException.class,()->service.applySavedCredential(request));}
    @Test void shortAccountNamesAreMaskedAndIncompleteCipherPairsRemainUnconfigured(){
      var mapper=mock(ClassTimetableMapper.class);var stored=new HashMap<String,Object>();
      when(mapper.getCrawlerCredential("default")).thenAnswer(call->stored.isEmpty()?null:stored);
      when(mapper.upsertCrawlerCredential(anyString(),anyString(),anyString())).thenAnswer(call->{stored.put("usernameCipher",call.getArgument(1));stored.put("passwordCipher",call.getArgument(2));return 1;});
      var service=new AcademicCredentialServiceImpl(mapper,"0123456789abcdef0123456789abcdef");
      var request=new AcademicScheduleImportRequest();request.setUsername("ab");request.setPassword("secret");
      assertEquals("a***",service.saveCredential(request).getUsernameMasked());
      stored.remove("usernameCipher");assertFalse(service.getCredentialView("default").getConfigured());assertThrows(IllegalArgumentException.class,()->service.applySavedCredential(request));
      stored.clear();stored.put("usernameCipher","incomplete");assertFalse(service.getCredentialView("default").getConfigured());
      request.setPassword(" ");assertThrows(IllegalArgumentException.class,()->service.saveCredential(request));
      assertThrows(IllegalStateException.class,()->new AcademicCredentialServiceImpl(mapper,null));
    }}
