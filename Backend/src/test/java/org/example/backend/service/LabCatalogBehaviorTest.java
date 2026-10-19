package org.example.backend.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LabCatalogBehaviorTest {
    final LabRecordReadService records=mock(LabRecordReadService.class);
    final ManagerNamePort names=mock(ManagerNamePort.class);
    final DeviceCountPort counts=mock(DeviceCountPort.class);
    final LabCatalogQuery service=new DefaultLabCatalogQuery(records,names,counts);
    LabRecord lab(Long id,Long manager){return new LabRecord(id,"A101","机房","教学楼","1F","101","计算机",30,manager,"开放","备注",null,null);}
    @BeforeEach void setup(){when(names.findNames(anySet())).thenReturn(Map.of(7L,"管理员甲"));when(counts.countByLabIds(anySet())).thenReturn(Map.of(1L,20));}
    @Test void enrichesBatchMetadataWithoutLosingScopeOrOrdering(){when(records.findAll(7L)).thenReturn(List.of(lab(1L,7L),lab(2L,null)));List<LabView> found=service.list(LabAccessScope.managedBy(7));assertEquals(List.of(1L,2L),found.stream().map(LabView::id).toList());assertEquals("管理员甲",found.get(0).managerName());assertEquals(20,found.get(0).deviceCount());assertNull(found.get(1).managerName());assertEquals(0,found.get(1).deviceCount());verify(records).findAll(7L);verify(names).findNames(Set.of(7L));verify(counts).countByLabIds(Set.of(1L,2L));}
    @Test void emptyAndMissingDetailsDoNotRequestMetadata(){when(records.findAll(null)).thenReturn(List.of());assertEquals(List.of(),service.list(LabAccessScope.all()));assertNull(service.findById(9,LabAccessScope.all()));verifyNoInteractions(names,counts);when(records.findById(1,null)).thenReturn(lab(1L,null));assertEquals("A101",service.findById(1,LabAccessScope.all()).labCode());verifyNoInteractions(names);}
    @Test void rejectsNullInvalidAndDuplicateRecords(){when(records.findAll(null)).thenReturn(null);assertThrows(NullPointerException.class,()->service.list(LabAccessScope.all()));for(List<LabRecord> source:List.of(Arrays.asList((LabRecord)null),List.of(lab(0L,null)),List.of(lab(null,null)),List.of(lab(1L,null),lab(1L,null)))){when(records.findAll(null)).thenReturn(source);assertThrows(IllegalStateException.class,()->service.list(LabAccessScope.all()));}assertThrows(NullPointerException.class,()->service.list(null));}
    @Test void rejectsForeignOrInvalidMetadataKeys(){when(records.findAll(null)).thenReturn(List.of(lab(1L,7L)));when(names.findNames(anySet())).thenReturn(Map.of(99L,"foreign"));assertThrows(IllegalStateException.class,()->service.list(LabAccessScope.all()));when(names.findNames(anySet())).thenReturn(Map.of(7L,"管理员甲"));when(counts.countByLabIds(anySet())).thenReturn(Map.of(2L,1));assertThrows(IllegalStateException.class,()->service.list(LabAccessScope.all()));when(counts.countByLabIds(anySet())).thenReturn(Map.of(1L,-1));assertThrows(IllegalStateException.class,()->service.list(LabAccessScope.all()));when(counts.countByLabIds(anySet())).thenReturn(null);assertThrows(NullPointerException.class,()->service.list(LabAccessScope.all()));}
}
