package org.example.backend.config;
import org.example.backend.VO.LabOptionVO;
import org.example.backend.mapper.TeachingScheduleReadMapper;
import org.example.backend.service.*;
import org.example.backend.service.gateway.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadAdapterContractTest {
    @Test void labOptionJsonContractSupportsFrameworkConstructionAndExplicitValues(){ObjectMapper json=new ObjectMapper();LabOptionVO input=json.readValue("{\"id\":7,\"labName\":\"机房甲\"}",LabOptionVO.class);assertEquals(7L,input.getId());assertEquals("机房甲",input.getLabName());assertEquals(7L,json.valueToTree(new LabOptionVO(7L,"机房甲")).get("id").asLong());}
    @Test void gatewayRegistryNormalizesConfiguredProtocolsAndFallsBackToMock(){MockGatewayAdapter fallback=new MockGatewayAdapter();GatewayAdapter first=mock(GatewayAdapter.class),duplicate=mock(GatewayAdapter.class);when(first.protocol()).thenReturn("TCP-RAW");when(duplicate.protocol()).thenReturn("tcp_raw");GatewayAdapterRegistry registry=new GatewayAdapterRegistry(List.of(first,duplicate),fallback);assertSame(first,registry.resolve(" TCP-RAW ",true));for(String protocol:Arrays.asList(null,"","mock","unknown"))assertSame(fallback,registry.resolve(protocol,true));assertSame(fallback,registry.resolve("tcp_raw",false));}
    @Test void teachingReadAdapterForwardsScopeAndCriteriaWithoutChangingMapperResults(){TeachingScheduleReadMapper mapper=mock(TeachingScheduleReadMapper.class);TeachingScheduleReadService service=new TeachingScheduleReadService(mapper);var occupancy=new ScheduleOccupancyCriteria("2026秋",7,"星期一");var search=new ScheduleSearchCriteria("2026秋","教师甲",null,null,null,null,7,50);when(mapper.listSemesters()).thenReturn(List.of("2026秋"));assertEquals(List.of("2026秋"),service.listSemesters());service.findActiveCalendar();service.findOccupancyCandidates(occupancy);service.search(search);service.summary();verify(mapper).findActiveCalendar();verify(mapper).findOccupancyCandidates(occupancy);verify(mapper).search(search);verify(mapper).summary();}
    @Test void openApiPublishesStableTitleVersionAndContactMetadata(){var info=new SwaggerConfig().customOpenAPI().getInfo();assertEquals("智云实验室API",info.getTitle());assertEquals("1.0",info.getVersion());assertNotNull(info.getDescription());assertNotNull(info.getContact().getName());assertNotNull(info.getContact().getEmail());assertNotNull(info.getLicense().getName());}
}

