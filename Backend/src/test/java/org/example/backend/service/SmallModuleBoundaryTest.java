package org.example.backend.service;
import org.example.backend.entity.softwareEntity;
import org.example.backend.entity.RepairEntity;
import org.example.backend.mapper.softwareMapper;
import org.example.backend.mapper.RepairMapper;
import org.example.backend.service.impl.softwareServiceImpl;
import org.example.backend.service.impl.RepairServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class SmallModuleBoundaryTest {
 @Test void blankSoftwareAliasesFillFromTheirCounterpartsButExplicitValuesRemain(){
  var mapper=mock(softwareMapper.class);var events=mock(BusinessLoopService.class);var service=new softwareServiceImpl(mapper,events);
  var alias=new softwareEntity();alias.setName("IDE");alias.setSoftwareName(" ");alias.setVersion("1.0");alias.setSoftwareVersion(" ");alias.setLicense("GPL");alias.setLicenseInfo(" ");alias.setType("开发");alias.setRemark(" ");alias.setStatus(" ");
  service.InserterLabSoftware(alias);assertEquals("IDE",alias.getSoftwareName());assertEquals("1.0",alias.getSoftwareVersion());assertEquals("GPL",alias.getLicenseInfo());assertEquals("开发",alias.getRemark());assertEquals("待安装",alias.getStatus());
  var reverse=new softwareEntity();reverse.setSoftwareName("数据库");reverse.setName(" ");reverse.setSoftwareVersion("8");reverse.setVersion(" ");reverse.setLicenseInfo("MIT");reverse.setLicense(" ");reverse.setRemark("保留");reverse.setStatus("已安装");
  service.updateLabSoftware(2L,reverse);assertEquals("数据库",reverse.getName());assertEquals("8",reverse.getVersion());assertEquals("MIT",reverse.getLicense());assertEquals("保留",reverse.getRemark());assertNotNull(reverse.getInstallTime());
 }
 @Test void blankRepairFieldsUseDefaultsAndAbsentSavedRecordDoesNotEmitInventedEvent(){
  var mapper=mock(RepairMapper.class);var events=mock(BusinessLoopService.class);var service=new RepairServiceImpl(mapper,events);
  var r=new RepairEntity();r.setTicket(" ");r.setStatus(" ");r.setAssignee(" ");r.setResult(" ");
  assertNull(service.createRepair(r));assertFalse(r.getTicket().isBlank());assertEquals("待派单",r.getStatus());assertEquals("待分配",r.getAssignee());assertFalse(r.getResult().isBlank());
  verify(events,never()).recordEvent(anyString(),anyString(),anyString(),anyString(),anyMap());verify(events).syncDeviceAfterRepairChanged(null);
  var patch=new RepairEntity();patch.setStatus(" ");patch.setResult(" ");when(mapper.patchRepair(eq("R1"),any())).thenReturn(1);
  service.patchRepair("R1",patch,"维修中",50,"正在修复");assertEquals("维修中",patch.getStatus());assertEquals("正在修复",patch.getResult());
 }
 @Test void repairDeletionWithNoLegacyIdentifierStillRecordsRequestedTarget(){
  var mapper=mock(RepairMapper.class);var events=mock(BusinessLoopService.class);var r=new RepairEntity();r.setTicket(" ");r.setId(" ");when(mapper.getRepair("R1")).thenReturn(r);
  new RepairServiceImpl(mapper,events).deleteRepair("R1");verify(events).recordEvent(eq("repair"),eq("delete"),eq(""),eq("已删除"),anyMap());
 }
 @Test void blankAdjustmentValuesUseWorkflowDefaults(){
  var mapper=mock(org.example.backend.mapper.ScheduleAdjustmentMapper.class);var notices=mock(org.example.backend.mapper.NoticeMapper.class);var events=mock(BusinessLoopService.class);
  var r=new org.example.backend.entity.ScheduleAdjustmentEntity();r.setCourse(" ");r.setOriginalTime(" ");r.setTargetTime(" ");r.setReason(" ");r.setStatus(" ");r.setTagType(" ");r.setReviewer(" ");r.setTeacherName(" ");
  new org.example.backend.service.impl.ScheduleAdjustmentServiceImpl(mapper,notices,events).createAdjustment(r);
  assertEquals("未命名课程",r.getCourse());assertEquals("待确认",r.getOriginalTime());assertEquals("待确认",r.getTargetTime());assertEquals("教学安排调整",r.getReason());assertEquals("审核中",r.getStatus());assertEquals("warning",r.getTagType());assertEquals("教务处",r.getReviewer());assertEquals("任课教师",r.getTeacherName());
 }
 @Test void suppliedCourseEnvironmentDetailsAreRetainedAndConfirmedStatusIsInferred(){
  var mapper=mock(org.example.backend.mapper.CourseEnvironmentMapper.class);var templates=mock(org.example.backend.mapper.EnvironmentTemplateMapper.class);var notices=mock(org.example.backend.mapper.NoticeMapper.class);var events=mock(BusinessLoopService.class);
  var service=new org.example.backend.service.impl.CourseEnvironmentServiceImpl(mapper,templates,notices,events);
  var r=new org.example.backend.entity.CourseEnvironmentEntity();r.setCourseName("课程");r.setCourse("课程");r.setSoftwareRequirements("IDE");r.setSoftware("IDE");r.setProcessStatus("已确认");r.setStatus("已确认");r.setTeacherUserId(7);r.setClassName("一班");r.setUseTime("周一");r.setLabType("机房");
  r.setId(9);when(mapper.getCourseEnvironment(9)).thenReturn(r);service.InserterCourseEnvironment(r);assertEquals("已确认",r.getConfirmStatus());assertEquals(7,r.getTeacherUserId());assertEquals("一班",r.getClassName());assertEquals("周一",r.getUseTime());assertEquals("机房",r.getLabType());
  r.setProcessStatus("待确认");r.setStatus("待确认");r.setConfirmStatus(" ");service.InserterCourseEnvironment(r);assertEquals("待确认",r.getConfirmStatus());
 }}


