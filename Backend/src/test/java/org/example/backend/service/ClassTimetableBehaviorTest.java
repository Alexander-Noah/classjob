package org.example.backend.service;

import org.example.backend.entity.ClassTimetableEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.impl.ClassTimetableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.Trigger;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassTimetableBehaviorTest {
    @Test void weakCredentialEncryptionKeysCannotStartTheService() {
        for (String secret : Arrays.asList(null, " ", "change-me-".repeat(5), "short")) {
            assertThrows(IllegalStateException.class, () -> new ClassTimetableServiceImpl(mapper, teaching, environment, scheduler, secret));
        }
    }
    @Test void completedImportStaysSuccessfulWhenHistoryAndSummaryAreUnavailable() throws Exception {
        scripts();
        Path csv = Files.createDirectories(temp.resolve("课表导出结果")).resolve("班级课表标准数据.csv");
        Files.writeString(csv, "学年学期,班级,星期,开始节次,结束节次,课程名称,教师,教室\n2026秋,软件2401,星期一,1,2,程序设计,李老师,A101\n");
        Process process = mock(Process.class);
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        doThrow(new IllegalStateException("history offline")).when(mapper).updateCrawlerTask(anyMap());
        when(teaching.summary()).thenThrow(new IllegalStateException("summary offline"));
        try (var construction = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.environment()).thenReturn(new HashMap<>());
            when(builder.start()).thenReturn(process);
        })) {
            var result = service.triggerCrawler();
            assertEquals("success", result.get("status"));
            assertEquals(1, result.get("javaImportCount"));
            assertFalse((Boolean) service.getCrawlerConfig().get("running"));
        }
        verify(mapper).insertTimetable(any(ClassTimetableEntity.class));
    }
    @Test void scheduledLaunchFailuresAreContainedEvenWhenFailureHistoryCannotBeSaved() throws Exception {
        scripts();
        config.put("scheduleEnabled", true);
        service.initializeCrawlerScheduler();
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(runnable.capture(), any(Trigger.class));
        doThrow(new IllegalStateException("history offline")).when(mapper).updateCrawlerTask(anyMap());
        // The first insert starts the task; the second attempts to record the scheduling failure.
        when(mapper.insertCrawlerTask(anyMap())).thenReturn(1).thenThrow(new IllegalStateException("history offline"));
        try (var construction = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.environment()).thenReturn(new HashMap<>());
            when(builder.start()).thenThrow(new java.io.IOException("cannot launch"));
        })) {
            assertDoesNotThrow(() -> runnable.getValue().run());
        }
        assertFalse((Boolean) service.getCrawlerConfig().get("running"));
        verify(mapper, times(2)).insertCrawlerTask(anyMap());
    }
    @Test void persistedScheduleFlagsAcceptLegacyTextAndNumbers() {
        for (Object flag : List.of("false", "0", "否", 0)) {
            config.put("scheduleEnabled", flag);
            assertEquals(false, service.getCrawlerConfig().get("scheduleEnabled"));
        }
        for (Object flag : List.of("true", "1", "是", 1, "unknown")) {
            config.put("scheduleEnabled", flag);
            assertEquals(true, service.getCrawlerConfig().get("scheduleEnabled"));
        }
    }
    final ClassTimetableMapper mapper=mock(ClassTimetableMapper.class);
    final TeachingScheduleQuery teaching=mock(TeachingScheduleQuery.class);
    final ThreadPoolTaskScheduler scheduler=mock(ThreadPoolTaskScheduler.class);
    final MockEnvironment environment=new MockEnvironment();
    final Map<String,Object> config=new HashMap<>();
    Map<String,Object> credential;
    ClassTimetableServiceImpl service;
    @TempDir Path temp;
    @BeforeEach void setup(){
        config.putAll(Map.of("scheduleEnabled",false,"cron","0 0 */6 * * *","semester","2026秋","semesterStartDate","2026-09-01"));
        when(mapper.getCrawlerConfig(anyString())).thenAnswer(call->new HashMap<>(config));
        when(mapper.upsertCrawlerConfig(anyString(),anyBoolean(),anyString(),nullable(String.class),nullable(java.time.LocalDate.class))).thenAnswer(call->{config.put("scheduleEnabled",call.getArgument(1));config.put("cron",call.getArgument(2));config.put("semester",call.getArgument(3));config.put("semesterStartDate",call.getArgument(4));return 1;});
        when(mapper.getCrawlerCredential(anyString())).thenAnswer(call->credential);
        when(mapper.upsertCrawlerCredential(anyString(),anyString(),anyString())).thenAnswer(call->{credential=new HashMap<>(Map.of("usernameCipher",call.getArgument(1),"passwordCipher",call.getArgument(2),"updatedAt","2026-10-18"));return 1;});
        service=new ClassTimetableServiceImpl(mapper,teaching,environment,scheduler,"0123456789abcdef0123456789abcdef");
    }
    @Test void credentialsAreEncryptedMaskedAndRecoverableAndCanBeRemoved(){
        assertFalse(service.getCrawlerCredential().getConfigured());assertThrows(IllegalArgumentException.class,()->service.saveCrawlerCredential(null));
        var view=service.saveCrawlerCredential(Map.of("username"," teacher ","password"," secret "));assertTrue(view.getUsable());assertEquals("te****er",view.getUsernameMasked());assertEquals("ready",view.getStatus());assertFalse(credential.toString().contains("secret"));String encrypted=(String)credential.get("passwordCipher");service.saveCrawlerCredential(Map.of("username","teacher","password","secret"));assertNotEquals(encrypted,credential.get("passwordCipher"));
        service.saveCrawlerCredential(Map.of("username","abc","password","secret"));assertEquals("a***",service.getCrawlerCredential().getUsernameMasked());service.deleteCrawlerCredential();verify(mapper).deleteCrawlerCredential("lab-admin-class-timetable");
    }
    @Test void corruptedAndPartiallyMissingCredentialsRequireUpdate(){
        credential=new HashMap<>(Map.of("usernameCipher","invalid","passwordCipher","invalid"));assertEquals("needs_update",service.getCrawlerCredential().getStatus());assertFalse(service.getCrawlerCredential().getUsable());assertThrows(CrawlerCredentialException.class,service::triggerCrawler);
        credential.remove("passwordCipher");assertEquals("needs_update",service.getCrawlerCredential().getStatus());assertThrows(IllegalArgumentException.class,service::triggerCrawler);
        credential=null;assertThrows(IllegalArgumentException.class,service::triggerCrawler);
    }
    @Test void configurationValidatesRequiredFieldsAndSchedulesReplacingOldFuture(){
        ScheduledFuture<?> future=mock(ScheduledFuture.class);doReturn(future).when(scheduler).schedule(any(Runnable.class),any(Trigger.class));
        Map<String,Object> payload=new HashMap<>(Map.of("scheduleEnabled","是","cron","0 30 8 * * *","semester"," 2026秋 ","semesterStartDate","2026-09-01"));
        var result=service.saveCrawlerConfig(payload);assertEquals(true,result.get("scheduleEnabled"));assertFalse(((String)result.get("nextRunAt")).isBlank());assertEquals(false,result.get("running"));
        payload.put("scheduleEnabled",0);result=service.saveCrawlerConfig(payload);assertEquals("",result.get("nextRunAt"));verify(future).cancel(false);
        for(String key:List.of("cron","semester","semesterStartDate")){Map<String,Object> invalid=new HashMap<>(payload);invalid.put(key,"");assertThrows(IllegalArgumentException.class,()->service.saveCrawlerConfig(invalid));}
        payload.put("cron","invalid");assertThrows(IllegalArgumentException.class,()->service.saveCrawlerConfig(payload));payload.put("cron","0 0 2 * * *");payload.put("semesterStartDate","bad");assertThrows(IllegalArgumentException.class,()->service.saveCrawlerConfig(payload));
    }
    @Test void initializesDefaultsAndRecoversInterruptedTasks(){
        when(mapper.getCrawlerConfig(anyString())).thenReturn(null);when(teaching.listSemesters()).thenReturn(List.of("2026秋"));service.initializeCrawlerScheduler();verify(mapper).upsertCrawlerConfig("lab-admin-class-timetable",true,"0 0 */6 * * *","2026秋",null);verify(mapper).failInterruptedCrawlerTasks();verify(scheduler).schedule(any(Runnable.class),any(Trigger.class));
        doThrow(new IllegalStateException("offline")).when(mapper).failInterruptedCrawlerTasks();assertDoesNotThrow(service::initializeCrawlerScheduler);
    }
    @Test void taskHistoryBoundsLimitAndReadsOnlyMissingOutputFromLogTail() throws Exception{
        Path log=temp.resolve("crawler.log");Files.writeString(log,"a".repeat(3100)+"end");Map<String,Object> task=new HashMap<>(Map.of("id",1,"logPath",log.toString(),"output",""));when(mapper.listCrawlerTasks(anyInt())).thenReturn(List.of(task));assertEquals(3000,((String)service.listCrawlerTasks(999).get(0).get("output")).length());assertTrue(((String)task.get("output")).endsWith("end"));verify(mapper).listCrawlerTasks(200);service.listCrawlerTasks(null);verify(mapper).listCrawlerTasks(50);service.listCrawlerTasks(0);service.clearCrawlerTasks();verify(mapper).clearCrawlerTasks();
    }
    void scripts() throws Exception{
        Path crawl=Files.writeString(temp.resolve("crawl.py"),"# mocked");Path importer=Files.writeString(temp.resolve("import.py"),"# mocked");environment.withProperty("timetable.crawler.python","python-test").withProperty("timetable.crawler.script-path",crawl.toString()).withProperty("timetable.crawler.import-script-path",importer.toString()).withProperty("timetable.crawler.working-dir",temp.toString());service.saveCrawlerCredential(Map.of("username","teacher","password","secret"));
    }
    @Test void relativeScriptPathsResolveThroughSiblingPythonDirectoryAndDefaultExecutable() throws Exception{service.saveCrawlerCredential(Map.of("username","teacher","password","secret"));environment.withProperty("timetable.crawler.script-path","missing-parent/crawl_school_timetable.py").withProperty("timetable.crawler.import-script-path","missing-parent/parse_and_import_timetable.py").withProperty("timetable.crawler.working-dir",temp.toString());Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(true);when(process.exitValue()).thenReturn(1);try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){var result=service.triggerCrawler();assertEquals("failed",result.get("status"));assertTrue(result.get("scriptPath").toString().endsWith("crawl_school_timetable.py"));assertEquals(1,construction.constructed().size());}}
    @Test void importFailureIsReportedWithoutImportingOldCsv() throws Exception{scripts();Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(true);when(process.exitValue()).thenReturn(0,2);try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){var result=service.triggerCrawler();assertEquals("failed",result.get("status"));assertEquals(2,result.get("exitCode"));assertTrue(result.get("message").toString().contains("导入失败"));}verify(mapper,never()).deleteBySemester(anyString());}
    @Test void interruptedCrawlPreservesInterruptAndRecordsTerminalFailure() throws Exception{scripts();Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenThrow(new InterruptedException());try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){try{assertThrows(IllegalStateException.class,service::triggerCrawler);assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}}verify(mapper).updateCrawlerTask(argThat(task->"failed".equals(task.get("status"))&&task.get("error").toString().contains("中断")));}
    @Test void absentConfiguredScriptsFailBeforeAnyProcessStarts() throws Exception{scripts();environment.withProperty("timetable.crawler.script-path",temp.resolve("missing.py").toString());assertTrue(assertThrows(IllegalArgumentException.class,service::triggerCrawler).getMessage().contains("爬虫脚本不存在"));environment.withProperty("timetable.crawler.script-path",temp.resolve("crawl.py").toString()).withProperty("timetable.crawler.import-script-path",temp.resolve("missing.py").toString());assertTrue(assertThrows(IllegalArgumentException.class,service::triggerCrawler).getMessage().contains("导入脚本不存在"));}
    @Test void emptyCsvCannotEraseExistingSemesterAndLogFallbackAcceptsGbk() throws Exception{scripts();Path dir=Files.createDirectories(temp.resolve("课表导出结果"));Files.writeString(dir.resolve("班级课表标准数据.csv"),"学年学期,班级\n");Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(true);when(process.exitValue()).thenReturn(0);try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){assertThrows(IllegalStateException.class,service::triggerCrawler);}verify(mapper,never()).deleteBySemester(anyString());Path log=temp.resolve("gbk.log");Files.write(log,"课表完成".getBytes(java.nio.charset.Charset.forName("GBK")));Map<String,Object> task=new HashMap<>(Map.of("id",1,"logPath",log.toString()));when(mapper.listCrawlerTasks(anyInt())).thenReturn(List.of(task));assertEquals("课表完成",service.listCrawlerTasks(1).get(0).get("output"));}
    @Test void successfulCollectionImportsCsvAndPersistsTerminalTaskBeforeSummary() throws Exception{
        scripts();Path csvDir=Files.createDirectories(temp.resolve("课表导出结果"));Files.writeString(csvDir.resolve("班级课表标准数据.csv"),"\uFEFF学年学期,班级,星期,开始节次,结束节次,课程名称,教师,教室\n2026秋,软件2401,星期一,1.0,2,\"程序,设计\",\"李\"\"老师\",A101\n\n2026秋,软件2401,星期二,bad,,数据库\n,班级,星期三,1,2,无学期\n");
        Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(true);when(process.exitValue()).thenReturn(0);when(teaching.summary()).thenReturn(new ScheduleSummaryView(2,1,1,1,null,List.of("2026秋")));
        try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){Map<String,Object> result=service.triggerCrawler();assertEquals("success",result.get("status"));assertEquals(2,result.get("javaImportCount"));assertEquals(2,construction.constructed().size());for(ProcessBuilder builder:construction.constructed()){assertEquals("secret",builder.environment().get("TIMETABLE_PASSWORD"));verify(builder).directory(temp.toFile());}assertFalse(((String)result.get("output")).contains("secret"));}
        var entities=ArgumentCaptor.forClass(ClassTimetableEntity.class);verify(mapper,times(2)).insertTimetable(entities.capture());assertEquals("程序,设计",entities.getAllValues().get(0).getCourseName());assertEquals("李\"老师",entities.getAllValues().get(0).getTeacher());assertNull(entities.getAllValues().get(1).getStartSection());verify(mapper).deleteBySemester("2026秋");var order=inOrder(mapper,teaching);order.verify(mapper).updateCrawlerTask(argThat(task->"success".equals(task.get("status"))&&Integer.valueOf(2).equals(task.get("updatedCount"))));order.verify(teaching).summary();
    }
    @ParameterizedTest @ValueSource(ints={1,-1}) void failedOrTimedOutCrawlDoesNotStartImporter(int exit) throws Exception{
        scripts();Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(exit!=-1);when(process.exitValue()).thenReturn(exit);
        try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){Map<String,Object> result=service.triggerCrawler();assertEquals(exit==-1?"timeout":"failed",result.get("status"));assertEquals(1,construction.constructed().size());assertEquals(false,service.getCrawlerConfig().get("running"));}verify(mapper,never()).deleteBySemester(anyString());if(exit==-1)verify(process).destroyForcibly();
    }
    @Test void invalidPersistedCronAndAbsentFlagsHaveSafeDisplayDefaults() {
        assertThrows(IllegalArgumentException.class,()->service.saveCrawlerConfig(null));
        config.remove("scheduleEnabled");config.put("cron","invalid");
        assertEquals(true,service.getCrawlerConfig().get("scheduleEnabled"));
        assertEquals("",service.getCrawlerConfig().get("nextRunAt"));
        config.put("cron","0 0 0 31 2 *");
        assertEquals("",service.getCrawlerConfig().get("nextRunAt"));
        assertThrows(IllegalStateException.class,()->new ClassTimetableServiceImpl(mapper,teaching,environment,scheduler,"dev-secret-".repeat(5)));
        for(Map<String,String> invalid:List.of(Map.of("username","teacher","password"," "),Map.of("username"," ","password","secret"))) {
            assertThrows(IllegalArgumentException.class,()->service.saveCrawlerCredential(invalid));
        }
    }
    @Test void missingAndPartialStoredCredentialsDoNotPretendToBeUsable() {
        credential=new HashMap<>();
        assertFalse(service.getCrawlerCredential().getConfigured());assertThrows(IllegalArgumentException.class,service::triggerCrawler);
        credential.put("passwordCipher","invalid");
        assertTrue(service.getCrawlerCredential().getConfigured());assertFalse(service.getCrawlerCredential().getUsable());
        assertThrows(IllegalArgumentException.class,service::triggerCrawler);
    }
    @Test void missingOrUnreadableLogPathsKeepTaskHistoryAvailable() throws Exception {
        var persisted=new HashMap<String,Object>(Map.of("output","already stored","logPath",temp.resolve("missing").toString()));
        var noPath=new HashMap<String,Object>(Map.of("output",""));
        var missing=new HashMap<String,Object>(Map.of("logPath",temp.resolve("missing").toString()));
        var directory=new HashMap<String,Object>(Map.of("logPath",temp.toString()));
        when(mapper.listCrawlerTasks(anyInt())).thenReturn(List.of(persisted,noPath,missing,directory));
        var history=service.listCrawlerTasks(4);
        assertEquals("already stored",history.get(0).get("output"));assertEquals("",history.get(1).get("output"));
        assertEquals("",history.get(2).get("output"));assertEquals("",history.get(3).get("output"));
    }
    @Test void defaultSemesterInitializationToleratesMissingOrUnavailableCatalog() {
        when(mapper.getCrawlerConfig(anyString())).thenReturn(null);
        when(teaching.listSemesters()).thenReturn(null,List.of()).thenThrow(new IllegalStateException("catalog offline"));
        for(int attempt=0;attempt<3;attempt++)assertDoesNotThrow(service::getCrawlerConfig);
        verify(mapper,times(3)).upsertCrawlerConfig("lab-admin-class-timetable",true,"0 0 */6 * * *",null,null);
    }
    @Test void concurrentManualAndScheduledCrawlsAreRejectedWithoutStartingAnotherProcess() throws Exception {
        scripts();config.put("scheduleEnabled",true);service.initializeCrawlerScheduler();
        var scheduled=ArgumentCaptor.forClass(Runnable.class);verify(scheduler).schedule(scheduled.capture(),any(Trigger.class));
        Process process=mock(Process.class);when(process.exitValue()).thenReturn(1);
        when(process.waitFor(anyLong(),any(TimeUnit.class))).thenAnswer(call->{
            assertTrue(assertThrows(IllegalStateException.class,service::triggerCrawler).getMessage().contains("正在执行"));
            scheduled.getValue().run();return true;
        });
        try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})) {
            assertEquals("failed",service.triggerCrawler().get("status"));assertEquals(1,construction.constructed().size());
        }
        verify(mapper).insertCrawlerTask(argThat(task->"skipped".equals(task.get("status"))));
        assertEquals(false,service.getCrawlerConfig().get("running"));
    }
    @Test void missingCsvMarksTaskFailedAndReleasesRunningLock() throws Exception{
        scripts();Process process=mock(Process.class);when(process.waitFor(anyLong(),any(TimeUnit.class))).thenReturn(true);when(process.exitValue()).thenReturn(0);
        try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenReturn(process);})){assertThrows(IllegalStateException.class,service::triggerCrawler);assertFalse((Boolean)service.getCrawlerConfig().get("running"));}verify(mapper).updateCrawlerTask(argThat(task->"failed".equals(task.get("status"))&&task.get("error").toString().contains("CSV")));
    }
    @Test void processStartFailuresAreRecordedAndScheduledMissingCredentialsDoNotEscape() throws Exception{
        scripts();try(var construction=mockConstruction(ProcessBuilder.class,(builder,context)->{when(builder.environment()).thenReturn(new HashMap<>());when(builder.start()).thenThrow(new java.io.IOException("cannot launch"));})){assertThrows(IllegalStateException.class,service::triggerCrawler);}verify(mapper).updateCrawlerTask(argThat(task->task.get("error").toString().contains("cannot launch")));
        credential=null;config.put("scheduleEnabled",true);service.initializeCrawlerScheduler();var runnable=ArgumentCaptor.forClass(Runnable.class);verify(scheduler).schedule(runnable.capture(),any(Trigger.class));assertDoesNotThrow(()->runnable.getValue().run());verify(mapper).insertCrawlerTask(argThat(task->"scheduled".equals(task.get("triggerType"))&&"failed".equals(task.get("status"))));
    }
}
