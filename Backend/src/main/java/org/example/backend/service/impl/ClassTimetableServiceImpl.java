package org.example.backend.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.AcademicCredentialView;
import org.example.backend.entity.ClassTimetableEntity;
import org.example.backend.mapper.ClassTimetableMapper;
import org.example.backend.service.ClassTimetableService;
import org.example.backend.service.CrawlerCredentialException;
import org.example.backend.service.TeachingScheduleQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 编排手动与定时课表采集：解密凭据、执行 Python、导入标准 CSV，并维护任务记录。
 * Python 负责浏览器导航和文件转换，数据库写入由本服务统一处理。
 * 调度与运行锁按单个后端实例管理，不是跨实例的分布式任务锁。
 */
@Service
@Slf4j
public class ClassTimetableServiceImpl implements ClassTimetableService {
    private static final int DEFAULT_TASK_LIMIT = 50;
    private static final int MAX_TASK_LIMIT = 200;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CRAWLER_CREDENTIAL_KEY = "lab-admin-class-timetable";
    private static final String CRAWLER_CONFIG_KEY = "lab-admin-class-timetable";
    private static final String DEFAULT_CRON = "0 0 */6 * * *";
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ClassTimetableMapper classTimetableMapper;
    private final TeachingScheduleQuery teachingScheduleQuery;
    private final Environment environment;
    private final SecretKeySpec secretKey;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final AtomicBoolean crawlerRunning = new AtomicBoolean(false);
    private final Object schedulerMonitor = new Object();
    private ScheduledFuture<?> scheduledCrawler;

    /**
     * 构造 ClassTimetableServiceImpl，初始化当前实例。
     * @param classTimetableMapper class课表数据映射器参数。
     * @param teachingScheduleQuery teaching课表查询参数。
     * @param environment 环境参数。
     * @param classTimetableTaskScheduler class课表TaskScheduler参数。
     * @param secret 秘密配置参数。
     */
    public ClassTimetableServiceImpl(
            ClassTimetableMapper classTimetableMapper,
            TeachingScheduleQuery teachingScheduleQuery,
            Environment environment,
            ThreadPoolTaskScheduler classTimetableTaskScheduler,
            @Value("${timetable.crawler.credential-secret:${ACADEMIC_CREDENTIAL_SECRET:}}") String secret
    ) {
        this.classTimetableMapper = classTimetableMapper;
        this.teachingScheduleQuery = teachingScheduleQuery;
        this.environment = environment;
        this.taskScheduler = classTimetableTaskScheduler;
        this.secretKey = buildSecretKey(requireStrongSecret(secret, "timetable.crawler.credential-secret"));
    }

    /**
     * 要求提供Strong秘密配置。
     * @param value 值参数。
     * @param name 名称参数。
     * @return String 类型的处理结果。
     */
    private String requireStrongSecret(String value, String name) {
        String secret = value == null ? "" : value.trim();
        if (secret.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        if (secret.toLowerCase().contains("change-me") || secret.toLowerCase().contains("dev-secret")) {
            throw new IllegalStateException(name + " must not use the development default");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(name + " must be at least 32 characters");
        }
        return secret;
    }

    /**
     * 初始化CrawlerScheduler。
     */
    @PostConstruct
    public void initializeCrawlerScheduler() {
        try {
            ensureDefaultCrawlerConfig();
            // 进程重启后旧任务已无执行线程，先收尾再注册下一次定时任务。
            classTimetableMapper.failInterruptedCrawlerTasks();
            rescheduleCrawler();
        } catch (RuntimeException exception) {
            log.warn("课表定时抓取初始化失败，保存配置后会再次尝试初始化", exception);
        }
    }

    /**
     * 获取Crawler配置。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> getCrawlerConfig() {
        ensureDefaultCrawlerConfig();
        Map<String, Object> config = normalizeCrawlerConfig(classTimetableMapper.getCrawlerConfig(CRAWLER_CONFIG_KEY));
        config.put("nextRunAt", computeNextRunAt(config));
        config.put("running", crawlerRunning.get());
        return config;
    }

    /**
     * 保存Crawler配置。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> saveCrawlerConfig(Map<String, Object> payload) {
        boolean scheduleEnabled = toBoolean(payload == null ? null : payload.get("scheduleEnabled"), true);
        String cron = normalizeCron(stringValue(payload == null ? null : payload.get("cron")));
        String semester = trimToNull(stringValue(payload == null ? null : payload.get("semester")));
        if (semester == null) {
            throw new IllegalArgumentException("请选择课表学期");
        }
        LocalDate semesterStartDate = normalizeSemesterStartDate(stringValue(payload == null ? null : payload.get("semesterStartDate")));

        classTimetableMapper.upsertCrawlerConfig(CRAWLER_CONFIG_KEY, scheduleEnabled, cron, semester, semesterStartDate);
        rescheduleCrawler();
        return getCrawlerConfig();
    }

    /**
     * 列出CrawlerTasks。
     * @param limit 上限参数。
     * @return 当前操作得到的记录集合。
     */
    @Override
    public List<Map<String, Object>> listCrawlerTasks(Integer limit) {
        List<Map<String, Object>> tasks = classTimetableMapper.listCrawlerTasks(normalizeTaskLimit(limit));
        for (Map<String, Object> task : tasks) {
            if (isBlank(stringValue(task.get("output"))) && !isBlank(stringValue(task.get("logPath")))) {
                task.put("output", readLogTail(Path.of(stringValue(task.get("logPath")))));
            }
        }
        return tasks;
    }

    /**
     * 清理CrawlerTasks。
     */
    @Override
    public void clearCrawlerTasks() {
        classTimetableMapper.clearCrawlerTasks();
    }

    /**
     * triggerCrawler的处理逻辑。
     * @return 当前操作返回的键值结果。
     */
    @Override
    public Map<String, Object> triggerCrawler() {
        return runCrawlerTask("manual");
    }

    /**
     * 运行CrawlerTask。
     * @param triggerType trigger类型参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> runCrawlerTask(String triggerType) {
        // 手动和定时共用同一把锁，避免同时使用浏览器用户目录及导出文件。
        if (!crawlerRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("课表抓取任务正在执行，请稍后再试");
        }
        try {
            return executeCrawlerTask(triggerType);
        } finally {
            crawlerRunning.set(false);
        }
    }

    /**
     * 执行CrawlerTask。
     * @param triggerType trigger类型参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> executeCrawlerTask(String triggerType) {
        StoredCrawlerCredential credential = readStoredCrawlerCredential();
        if (credential == null) {
            throw new IllegalArgumentException("请先在课表抓取页面保存教务系统账号和密码");
        }

        String python = resolvePythonExecutable();
        String scriptPathText = environment.getProperty("timetable.crawler.script-path", "Python/crawl_school_timetable.py");
        String importScriptPathText = environment.getProperty("timetable.crawler.import-script-path", "Python/parse_and_import_timetable.py");
        String workingDirText = environment.getProperty("timetable.crawler.working-dir", "Python");
        long timeoutSeconds = environment.getProperty("timetable.crawler.timeout-seconds", Long.class, 600L);

        Path scriptPath = resolveConfiguredScriptPath(scriptPathText);
        Path importScriptPath = resolveConfiguredScriptPath(importScriptPathText);
        Path workingDir = Path.of(workingDirText).toAbsolutePath().normalize();
        if (!Files.exists(scriptPath)) {
            throw new IllegalArgumentException("课表爬虫脚本不存在：" + scriptPath);
        }
        if (!Files.exists(importScriptPath)) {
            throw new IllegalArgumentException("课表导入脚本不存在：" + importScriptPath);
        }

        Path logPath = buildCrawlerLogPath();
        Map<String, Object> crawlerConfig = getCrawlerConfig();
        Map<String, Object> task = insertStartedCrawlerTask(triggerType, credential, logPath);

        // 凭据通过子进程环境传递，避免拼进命令行参数或任务日志。
        Map<String, String> crawlerEnv = new HashMap<>();
        crawlerEnv.put("TIMETABLE_USERNAME", credential.username());
        crawlerEnv.put("TIMETABLE_PASSWORD", credential.password());
        crawlerEnv.put("TIMETABLE_HEADLESS", environment.getProperty("timetable.crawler.headless", "true"));
        crawlerEnv.put("TIMETABLE_SEMESTER", stringValue(crawlerConfig.get("semester")));
        crawlerEnv.put("TIMETABLE_SEMESTER_START_DATE", stringValue(crawlerConfig.get("semesterStartDate")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.get("id"));
        result.put("triggerType", triggerType);
        result.put("successAccount", maskUsername(credential.username()));
        result.put("scriptPath", scriptPath.toString());
        result.put("importScriptPath", importScriptPath.toString());
        result.put("workingDir", workingDir.toString());
        result.put("logPath", logPath.toString());
        result.put("startedAt", LocalDateTime.now().toString());

        try {
            int crawlExitCode = runPythonScript(python, scriptPath, workingDir, logPath, timeoutSeconds, false, crawlerEnv);
            result.put("crawlExitCode", crawlExitCode);

            // 只有浏览器采集成功才转换文件，避免沿用上一轮遗留的导出结果入库。
            if (crawlExitCode == 0) {
                int importExitCode = runPythonScript(python, importScriptPath, workingDir, logPath, timeoutSeconds, true, crawlerEnv);
                result.put("importExitCode", importExitCode);
                if (importExitCode == 0) {
                    int javaImportCount = importStandardCsv(workingDir, logPath);
                    result.put("javaImportCount", javaImportCount);
                    result.put("status", "success");
                    result.put("exitCode", importExitCode);
                    result.put("message", "课表爬取并导入完成");
                } else {
                    result.put("status", "failed");
                    result.put("exitCode", importExitCode);
                    result.put("message", "课表导入失败，请查看日志");
                }
            } else if (crawlExitCode == -1) {
                result.put("status", "timeout");
                result.put("exitCode", crawlExitCode);
                result.put("message", "课表爬虫执行超时，已终止进程");
            } else {
                result.put("status", "failed");
                result.put("exitCode", crawlExitCode);
                result.put("message", "课表爬取失败，未执行导入，请查看日志");
            }

            result.put("output", readLogTail(logPath));
            result.put("finishedAt", LocalDateTime.now().toString());
            updateFinishedCrawlerTask(task, result, credential, logPath);
            // 先持久化任务终态再读统计，统计查询故障不能让已结束任务一直显示运行中。
            if ("success".equals(result.get("status"))) {
                try {
                    result.put("summary", teachingScheduleQuery.summary());
                } catch (RuntimeException exception) {
                    log.warn("课表导入已完成，但查询课表统计失败", exception);
                }
            }
            return result;
        } catch (IOException exception) {
            updateFailedCrawlerTask(task, credential, logPath, "无法启动课表爬虫脚本：" + exception.getMessage());
            throw new IllegalStateException("无法启动课表爬虫脚本：" + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            // 保留线程中断语义，允许容器关闭/调度取消流程识别本次执行已经被打断。
            Thread.currentThread().interrupt();
            updateFailedCrawlerTask(task, credential, logPath, "课表爬虫脚本执行被中断");
            throw new IllegalStateException("课表爬虫脚本执行被中断", exception);
        } catch (RuntimeException exception) {
            updateFailedCrawlerTask(task, credential, logPath, exception.getMessage());
            throw exception;
        }
    }

    /**
     * 运行ScheduledCrawlerSafely。
     */
    private void runScheduledCrawlerSafely() {
        try {
            runCrawlerTask("scheduled");
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("正在执行")) {
                insertSimpleCrawlerTask("scheduled", "skipped", "已有课表抓取任务正在执行，本次定时触发已跳过");
                return;
            }
            insertSimpleCrawlerTask("scheduled", "failed", exception.getMessage());
            log.warn("课表定时抓取执行失败", exception);
        } catch (RuntimeException exception) {
            insertSimpleCrawlerTask("scheduled", "failed", exception.getMessage());
            log.warn("课表定时抓取执行失败", exception);
        }
    }

    /**
     * rescheduleCrawler的处理逻辑。
     */
    private void rescheduleCrawler() {
        // 保存配置后替换旧计划；cancel(false) 不打断已经开始的采集。
        synchronized (schedulerMonitor) {
            if (scheduledCrawler != null) {
                scheduledCrawler.cancel(false);
                scheduledCrawler = null;
            }

            Map<String, Object> config = normalizeCrawlerConfig(classTimetableMapper.getCrawlerConfig(CRAWLER_CONFIG_KEY));
            if (!toBoolean(config.get("scheduleEnabled"), true)) {
                return;
            }

            String cron = normalizeCron(stringValue(config.get("cron")));
            scheduledCrawler = taskScheduler.schedule(this::runScheduledCrawlerSafely, new CronTrigger(cron));
        }
    }

    /**
     * 插入StartedCrawlerTask。
     * @param triggerType trigger类型参数。
     * @param credential 凭据参数。
     * @param logPath log路径参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> insertStartedCrawlerTask(
            String triggerType,
            StoredCrawlerCredential credential,
            Path logPath
    ) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", nextTaskId());
        task.put("triggerType", triggerType);
        task.put("status", "running");
        task.put("scope", "class");
        task.put("source", "class-all");
        task.put("successAccount", maskUsername(credential.username()));
        task.put("attempts", 0);
        task.put("updatedCount", 0);
        task.put("startedAt", LocalDateTime.now());
        task.put("finishedAt", null);
        task.put("error", "");
        task.put("output", "");
        task.put("logPath", logPath.toString());
        classTimetableMapper.insertCrawlerTask(task);
        return task;
    }

    /**
     * 更新FinishedCrawlerTask。
     * @param task task参数。
     * @param result 结果参数。
     * @param credential 凭据参数。
     * @param logPath log路径参数。
     */
    private void updateFinishedCrawlerTask(
            Map<String, Object> task,
            Map<String, Object> result,
            StoredCrawlerCredential credential,
            Path logPath
    ) {
        try {
            String status = stringValue(result.get("status"));
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("id", task.get("id"));
            update.put("status", isBlank(status) ? "unknown" : status);
            update.put("successAccount", maskUsername(credential.username()));
            update.put("attempts", result.get("crawlExitCode") == null ? 0 : 1);
            update.put("updatedCount", extractUpdatedCount(result));
            update.put("finishedAt", LocalDateTime.now());
            update.put("error", "success".equals(status) ? "" : stringValue(result.get("message")));
            update.put("output", stringValue(result.get("output")));
            update.put("logPath", logPath.toString());
            classTimetableMapper.updateCrawlerTask(update);
        } catch (RuntimeException exception) {
            log.warn("更新课表抓取任务记录失败", exception);
        }
    }

    /**
     * 更新FailedCrawlerTask。
     * @param task task参数。
     * @param credential 凭据参数。
     * @param logPath log路径参数。
     * @param error 错误参数。
     */
    private void updateFailedCrawlerTask(
            Map<String, Object> task,
            StoredCrawlerCredential credential,
            Path logPath,
            String error
    ) {
        try {
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("id", task.get("id"));
            update.put("status", "failed");
            update.put("successAccount", credential == null ? "" : maskUsername(credential.username()));
            update.put("attempts", 1);
            update.put("updatedCount", 0);
            update.put("finishedAt", LocalDateTime.now());
            update.put("error", isBlank(error) ? "课表抓取失败" : error);
            update.put("output", readLogTail(logPath));
            update.put("logPath", logPath.toString());
            classTimetableMapper.updateCrawlerTask(update);
        } catch (RuntimeException exception) {
            log.warn("更新课表抓取失败记录失败", exception);
        }
    }

    /**
     * 插入SimpleCrawlerTask。
     * @param triggerType trigger类型参数。
     * @param status 状态参数。
     * @param error 错误参数。
     */
    private void insertSimpleCrawlerTask(String triggerType, String status, String error) {
        try {
            StoredCrawlerCredential credential = null;
            try {
                credential = readStoredCrawlerCredential();
            } catch (RuntimeException ignored) {
            }
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", nextTaskId());
            task.put("triggerType", triggerType);
            task.put("status", status);
            task.put("scope", "class");
            task.put("source", "class-all");
            task.put("successAccount", credential == null ? "" : maskUsername(credential.username()));
            task.put("attempts", 0);
            task.put("updatedCount", 0);
            task.put("startedAt", LocalDateTime.now());
            task.put("finishedAt", LocalDateTime.now());
            task.put("error", isBlank(error) ? status : error);
            task.put("output", "");
            task.put("logPath", "");
            classTimetableMapper.insertCrawlerTask(task);
        } catch (RuntimeException exception) {
            log.warn("写入课表定时抓取任务记录失败", exception);
        }
    }

    /**
     * 提取Updated数量。
     * @param result 结果参数。
     * @return int 类型的处理结果。
     */
    private int extractUpdatedCount(Map<String, Object> result) {
        Object javaImportCount = result.get("javaImportCount");
        if (javaImportCount instanceof Number number) {
            return number.intValue();
        }
        Object summary = result.get("summary");
        if (summary instanceof Map<?, ?> summaryMap) {
            Object total = summaryMap.get("total");
            if (total instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(stringValue(total));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 确保满足默认Crawler配置。
     */
    private void ensureDefaultCrawlerConfig() {
        if (classTimetableMapper.getCrawlerConfig(CRAWLER_CONFIG_KEY) == null) {
            classTimetableMapper.upsertCrawlerConfig(
                    CRAWLER_CONFIG_KEY,
                    true,
                    DEFAULT_CRON,
                    latestSemester(),
                    null
            );
        }
    }

    /**
     * 归一化Crawler配置。
     * @param row 行参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, Object> normalizeCrawlerConfig(Map<String, Object> row) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("configKey", CRAWLER_CONFIG_KEY);
        config.put("scheduleEnabled", true);
        config.put("cron", DEFAULT_CRON);
        config.put("semester", "");
        config.put("semesterStartDate", "");
        config.put("updatedAt", "");
        if (row != null) {
            config.put("scheduleEnabled", toBoolean(row.get("scheduleEnabled"), true));
            config.put("cron", isBlank(stringValue(row.get("cron"))) ? DEFAULT_CRON : stringValue(row.get("cron")));
            config.put("semester", stringValue(row.get("semester")));
            config.put(
                    "semesterStartDate",
                    isBlank(stringValue(row.get("semesterStartDate")))
                            ? ""
                            : stringValue(row.get("semesterStartDate"))
            );
            config.put("updatedAt", stringValue(row.get("updatedAt")));
        }
        return config;
    }

    /**
     * latest学期的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String latestSemester() {
        try {
            List<String> semesters = teachingScheduleQuery.listSemesters();
            return semesters == null || semesters.isEmpty() ? null : trimToNull(semesters.get(0));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 计算NextRunAt。
     * @param config 配置参数。
     * @return String 类型的处理结果。
     */
    private String computeNextRunAt(Map<String, Object> config) {
        if (!toBoolean(config.get("scheduleEnabled"), true)) {
            return "";
        }
        try {
            LocalDateTime next = CronExpression.parse(stringValue(config.get("cron"))).next(LocalDateTime.now());
            return next == null ? "" : next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    /**
     * 归一化Cron。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String normalizeCron(String value) {
        String cron = trimToNull(value);
        if (cron == null) {
            throw new IllegalArgumentException("请设置抓取频率");
        }
        try {
            CronExpression.parse(cron);
            return cron;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("抓取时间设置不合法：" + exception.getMessage(), exception);
        }
    }

    /**
     * 归一化学期开始日期。
     * @param value 值参数。
     * @return LocalDate 类型的处理结果。
     */
    private LocalDate normalizeSemesterStartDate(String value) {
        String semesterStartDate = trimToNull(value);
        if (semesterStartDate == null) {
            throw new IllegalArgumentException("请选择学期起始时间");
        }
        try {
            return LocalDate.parse(semesterStartDate);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("学期起始时间格式应为 YYYY-MM-DD", exception);
        }
    }

    /**
     * 转换为布尔值。
     * @param value 值参数。
     * @param defaultValue 默认值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = stringValue(value).trim();
        if (text.equalsIgnoreCase("true") || text.equals("1") || text.equals("是")) {
            return true;
        }
        if (text.equalsIgnoreCase("false") || text.equals("0") || text.equals("否")) {
            return false;
        }
        return defaultValue;
    }

    /**
     * 归一化Task上限。
     * @param limit 上限参数。
     * @return int 类型的处理结果。
     */
    private int normalizeTaskLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_TASK_LIMIT;
        }
        return Math.min(limit, MAX_TASK_LIMIT);
    }

    // 毫秒时间加随机尾码用于本地任务排序；不提供多实例下的严格全局唯一保证。
    private long nextTaskId() {
        return System.currentTimeMillis() * 1000 + secureRandom.nextInt(1000);
    }

    /**
     * 将当前导出的标准 CSV 按涉及学期替换课表数据；空表不触发删除。
     * 文件按物理行读取，因此字段不得包含 CSV 引号包裹的跨行换行。
     * 本私有方法没有自行开启事务，不能据此宣称删除和全部插入具有自动回滚保证。
     *
     * @param workingDir Python 工作目录，文件固定在其“课表导出结果”子目录
     * @param logPath 当前任务日志，写入导入数量或文件读取失败原因
     * @return 有效实体的插入数量；缺少学期或课程名的行会跳过
     * @throws IllegalStateException 文件缺失、仅有表头或 UTF-8 读取失败
     */
    private int importStandardCsv(Path workingDir, Path logPath) {
        Path csvPath = workingDir.resolve("课表导出结果").resolve("班级课表标准数据.csv").normalize();
        if (!Files.exists(csvPath)) {
            appendLog(logPath, "后端 JDBC 导入失败：未找到标准 CSV：" + csvPath);
            throw new IllegalStateException("未找到标准 CSV：" + csvPath);
        }

        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                appendLog(logPath, "后端 JDBC 导入失败：标准 CSV 没有课表数据");
                throw new IllegalStateException("标准 CSV 没有课表数据");
            }

            List<String> headers = parseCsvLine(stripBom(lines.get(0)));
            List<ClassTimetableEntity> entities = new ArrayList<>();
            Set<String> semesters = new LinkedHashSet<>();

            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) {
                    continue;
                }
                Map<String, String> row = toCsvRow(headers, parseCsvLine(lines.get(index)));
                ClassTimetableEntity entity = toTimetableEntity(row);
                if (isBlank(entity.getSemester()) || isBlank(entity.getCourseName())) {
                    continue;
                }
                entities.add(entity);
                semesters.add(entity.getSemester());
            }

            // 先解析完文件再决定替换哪些学期，未出现在有效数据中的学期保持原状。
            for (String semester : semesters) {
                classTimetableMapper.deleteBySemester(semester);
            }
            for (ClassTimetableEntity entity : entities) {
                classTimetableMapper.insertTimetable(entity);
            }

            appendLog(logPath, "后端 JDBC 导入完成，共导入 " + entities.size() + " 条课表数据");
            return entities.size();
        } catch (IOException exception) {
            appendLog(logPath, "后端 JDBC 导入失败：" + exception.getMessage());
            throw new IllegalStateException("读取标准 CSV 失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 转换为课表Entity。
     * @param row 行参数。
     * @return ClassTimetableEntity 类型的处理结果。
     */
    private ClassTimetableEntity toTimetableEntity(Map<String, String> row) {
        ClassTimetableEntity entity = new ClassTimetableEntity();
        entity.setSemester(row.getOrDefault("学年学期", ""));
        entity.setRowClassName(row.getOrDefault("行班级", ""));
        entity.setClassName(row.getOrDefault("班级", ""));
        entity.setWeekday(row.getOrDefault("星期", ""));
        entity.setSectionCode(row.getOrDefault("节次代码", ""));
        entity.setSectionText(row.getOrDefault("节次", ""));
        entity.setStartSection(parseInteger(row.get("开始节次")));
        entity.setEndSection(parseInteger(row.get("结束节次")));
        entity.setCourseName(row.getOrDefault("课程名称", ""));
        entity.setTeacher(row.getOrDefault("教师", ""));
        entity.setWeekRaw(row.getOrDefault("周次原文", ""));
        entity.setWeekText(row.getOrDefault("周次", ""));
        entity.setWeekExpanded(row.getOrDefault("展开周次", ""));
        entity.setClassroom(row.getOrDefault("教室", ""));
        return entity;
    }

    /**
     * 转换为CSV行。
     * @param headers 请求头参数。
     * @param values 值集合参数。
     * @return 当前操作返回的键值结果。
     */
    private Map<String, String> toCsvRow(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            row.put(headers.get(index), index < values.size() ? values.get(index) : "");
        }
        return row;
    }

    /**
     * 解析CSVLine。
     * @param line line参数。
     * @return 当前操作得到的记录集合。
     */
    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    /**
     * 解析Integer。
     * @param value 值参数。
     * @return Integer 类型的处理结果。
     */
    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * stripBom的处理逻辑。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    /**
     * 追加Log。
     * 涉及：写入文件。
     * @param logPath log路径参数。
     * @param message 消息参数。
     */
    private void appendLog(Path logPath, String message) {
        try {
            Files.writeString(
                    logPath,
                    System.lineSeparator() + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    Files.exists(logPath)
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE
            );
        } catch (IOException ignored) {
        }
    }

    /**
     * 获取Crawler凭据。
     * @return AcademicCredentialView 类型的处理结果。
     */
    @Override
    public AcademicCredentialView getCrawlerCredential() {
        Map<String, Object> row = classTimetableMapper.getCrawlerCredential(CRAWLER_CREDENTIAL_KEY);
        return toCredentialView(row);
    }

    /**
     * 保存Crawler凭据。
     * @param payload 载荷参数。
     * @return AcademicCredentialView 类型的处理结果。
     */
    @Override
    public AcademicCredentialView saveCrawlerCredential(Map<String, String> payload) {
        String username = trimToNull(payload == null ? null : payload.get("username"));
        String password = trimToNull(payload == null ? null : payload.get("password"));
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalArgumentException("请填写教务系统账号和密码");
        }

        classTimetableMapper.upsertCrawlerCredential(
                CRAWLER_CREDENTIAL_KEY,
                encrypt(username),
                encrypt(password)
        );
        return getCrawlerCredential();
    }

    /**
     * 删除Crawler凭据。
     */
    @Override
    public void deleteCrawlerCredential() {
        classTimetableMapper.deleteCrawlerCredential(CRAWLER_CREDENTIAL_KEY);
    }

    /**
     * 读取StoredCrawler凭据。
     * @return StoredCrawlerCredential 类型的处理结果。
     */
    private StoredCrawlerCredential readStoredCrawlerCredential() {
        Map<String, Object> row = classTimetableMapper.getCrawlerCredential(CRAWLER_CREDENTIAL_KEY);
        if (row == null || row.isEmpty()) {
            return null;
        }
        String usernameCipher = stringValue(row.get("usernameCipher"));
        String passwordCipher = stringValue(row.get("passwordCipher"));
        if (isBlank(usernameCipher) || isBlank(passwordCipher)) {
            return null;
        }
        return new StoredCrawlerCredential(decrypt(usernameCipher), decrypt(passwordCipher));
    }

    /**
     * 转换为凭据视图。
     * @param row 行参数。
     * @return AcademicCredentialView 类型的处理结果。
     */
    private AcademicCredentialView toCredentialView(Map<String, Object> row) {
        AcademicCredentialView view = new AcademicCredentialView();
        view.setCredentialKey(CRAWLER_CREDENTIAL_KEY);
        String usernameCipher = row == null ? "" : stringValue(row.get("usernameCipher"));
        String passwordCipher = row == null ? "" : stringValue(row.get("passwordCipher"));
        view.setConfigured(!isBlank(usernameCipher) || !isBlank(passwordCipher));
        view.setUsable(false);
        view.setStatus("not_configured");
        if (!Boolean.TRUE.equals(view.getConfigured())) return view;
        view.setUpdatedAt(stringValue(row.get("updatedAt")));
        view.setStatus("needs_update");
        view.setMessage(CrawlerCredentialException.USER_MESSAGE);
        if (isBlank(usernameCipher) || isBlank(passwordCipher)) return view;
        try {
            String username = decrypt(usernameCipher);
            decrypt(passwordCipher);
            view.setUsernameMasked(maskUsername(username));
            view.setUsable(true);
            view.setStatus("ready");
            view.setMessage(null);
        } catch (CrawlerCredentialException exception) {
            log.warn("Stored timetable credentials cannot be read; reconfiguration is required, cause={}",
                    exception.getCause() == null ? "unknown" : exception.getCause().getClass().getSimpleName());
        }
        return view;
    }

    /**
     * 启动一个脚本并等待退出；通过文件接收合并输出，不在 Java 内消费输出管道。
     *
     * @param python 解释器路径或命令名，作为独立 ProcessBuilder 参数使用
     * @param scriptPath 待执行脚本的绝对路径
     * @param workingDir 子进程工作目录，影响脚本的相对路径解析
     * @param logPath 任务日志文件；输出包含诊断路径，不包含传入的凭据环境变量
     * @param timeoutSeconds 当前脚本的等待上限，单位秒，并非整次采集的总时限
     * @param appendLog 是否在已有任务日志后追加脚本启动说明
     * @param extraEnv 仅传给子进程的配置，不修改当前 JVM 的环境变量
     * @return Python 退出码；等待超时返回服务内部约定值 -1
     * @throws IOException 日志写入或子进程创建失败
     * @throws InterruptedException 当前线程等待被中断，由外层恢复中断标记并记录失败
     */
    private int runPythonScript(
            String python,
            Path scriptPath,
            Path workingDir,
            Path logPath,
            long timeoutSeconds,
            boolean appendLog,
            Map<String, String> extraEnv
    ) throws IOException, InterruptedException {
        Files.writeString(
                logPath,
                "Python executable: " + python + System.lineSeparator()
                        + "Script path: " + scriptPath + System.lineSeparator()
                        + "Working dir: " + workingDir + System.lineSeparator(),
                StandardCharsets.UTF_8,
                appendLog ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        );
        // 标准输出和错误输出合并写入文件，避免管道未消费导致子进程阻塞。
        ProcessBuilder processBuilder = new ProcessBuilder(python, scriptPath.toString());
        processBuilder.directory(workingDir.toFile());
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().putAll(extraEnv);
        processBuilder.redirectErrorStream(true);
        if (appendLog) {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        } else {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        }

        Process process = processBuilder.start();
        // 超时按每个脚本分别计算，-1 是本服务约定的超时结果，不是 Python 退出码。
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            // 这里只请求终止直接子进程；浏览器资源的正常收尾仍依赖 Python 的 finally。
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }

    /**
     * 解析PythonExecutable。
     * @return String 类型的处理结果。
     */
    private String resolvePythonExecutable() {
        String configuredPython = environment.getProperty("timetable.crawler.python", "").trim();
        if (!configuredPython.isEmpty()) {
            return configuredPython;
        }

        List<Path> candidates = List.of(
                Path.of("Python/.venv/Scripts/python.exe"),
                Path.of("../Python/.venv/Scripts/python.exe"),
                Path.of("Python/.venv/bin/python"),
                Path.of("../Python/.venv/bin/python")
        );
        for (Path candidate : candidates) {
            Path pythonPath = candidate.toAbsolutePath().normalize();
            if (Files.exists(pythonPath)) {
                return pythonPath.toString();
            }
        }
        return "python";
    }

    /**
     * 解析ConfiguredScript路径。
     * @param scriptPathText script路径文本参数。
     * @return Path 类型的处理结果。
     */
    private Path resolveConfiguredScriptPath(String scriptPathText) {
        Path rawPath = Path.of(scriptPathText);
        Path configuredPath = rawPath.toAbsolutePath().normalize();
        if (Files.exists(configuredPath) || rawPath.isAbsolute()) {
            return configuredPath;
        }

        Path fileName = rawPath.getFileName();
        if (fileName == null) {
            return configuredPath;
        }

        Path rootPythonPath = Path.of("Python").resolve(fileName).toAbsolutePath().normalize();
        if (Files.exists(rootPythonPath)) {
            return rootPythonPath;
        }

        Path backendSiblingPythonPath = Path.of("../Python").resolve(fileName).toAbsolutePath().normalize();
        if (Files.exists(backendSiblingPythonPath)) {
            return backendSiblingPythonPath;
        }

        return configuredPath;
    }

    /**
     * 构建CrawlerLog路径。
     * @return Path 类型的处理结果。
     */
    private Path buildCrawlerLogPath() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path logDir = Path.of("logs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(logDir);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建课表爬虫日志目录", exception);
        }
        return logDir.resolve("class-timetable-crawler-" + timestamp + ".log");
    }

    /**
     * 读取LogTail。
     * @param logPath log路径参数。
     * @return String 类型的处理结果。
     */
    private String readLogTail(Path logPath) {
        try {
            return tailText(Files.readString(logPath, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            try {
                return tailText(Files.readString(logPath, Charset.forName("GBK")));
            } catch (IOException ignored) {
                return "";
            }
        }
    }

    /**
     * tail文本的处理逻辑。
     * @param content 内容参数。
     * @return String 类型的处理结果。
     */
    private String tailText(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 3000) {
            return content;
        }
        return content.substring(content.length() - 3000);
    }

    /**
     * 加密当前操作。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("教务账号加密失败", exception);
        }
    }

    /**
     * 解密当前操作。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String decrypt(String value) {
        try {
            byte[] payload = Base64.getDecoder().decode(value);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new CrawlerCredentialException(exception);
        }
    }

    /**
     * 构建秘密配置键。
     * @param secret 秘密配置参数。
     * @return SecretKeySpec 类型的处理结果。
     */
    private SecretKeySpec buildSecretKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("初始化教务账号加密密钥失败", exception);
        }
    }

    /**
     * maskUsername的处理逻辑。
     * @param username username参数。
     * @return String 类型的处理结果。
     */
    private String maskUsername(String username) {
        if (isBlank(username)) {
            return "";
        }
        if (username.length() <= 4) {
            return username.charAt(0) + "***";
        }
        return username.substring(0, 2) + "****" + username.substring(username.length() - 2);
    }

    /**
     * 裁剪目标Null。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 判断是否为空白。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 将输入转换为字符串值。
     * @param value 值参数。
     * @return String 类型的处理结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record StoredCrawlerCredential(String username, String password) {
    }
}
