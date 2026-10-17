/*
 * Vosk 语音识别实现模块。
 * 主要协作组件：ObjectMapper。
 */
package org.example.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.SpeechRecognitionService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
public class VoskSpeechRecognitionServiceImpl implements SpeechRecognitionService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final float SAMPLE_RATE = 16000.0f;
    private static final String DEFAULT_MODEL_PATH = "../vosk-model-small-cn-0.22/vosk-model-small-cn-0.22";

    private final ObjectMapper objectMapper;
    private final Model model;

    /**
     * 构造 VoskSpeechRecognitionServiceImpl，初始化当前实例。
     * @param objectMapper object数据映射器参数。
     * @param environment 环境参数。
     */
    public VoskSpeechRecognitionServiceImpl(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.model = loadModel(environment);
    }

    /**
     * 判断是否为可用。
     * @return 当前判断的布尔结果。
     */
    @Override
    public boolean isAvailable() {
        return model != null;
    }

    /**
     * 创建Recognizer。
     * @return Recognizer 类型的处理结果。
     */
    @Override
    public Recognizer createRecognizer() {
        if (model == null) {
            throw new IllegalStateException("Vosk 语音模型不可用");
        }
        try {
            return new Recognizer(model, SAMPLE_RATE);
        } catch (Exception exception) {
            throw new IllegalStateException("无法创建 Vosk 识别器", exception);
        }
    }

    /**
     * 获取结果文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String getResultText(Recognizer recognizer) {
        if (recognizer == null) {
            return "";
        }
        return readText(recognizer.getResult(), "text", "Vosk 结果");
    }

    /**
     * 获取Partial文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String getPartialText(Recognizer recognizer) {
        if (recognizer == null) {
            return "";
        }
        return readText(recognizer.getPartialResult(), "partial", "Vosk 部分结果");
    }

    /**
     * 获取Final文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    @Override
    public String getFinalText(Recognizer recognizer) {
        if (recognizer == null) {
            return "";
        }
        return readText(recognizer.getFinalResult(), "text", "Vosk 最终结果");
    }

    /**
     * 读取文本。
     * @param resultPayload 结果载荷参数。
     * @param textKey 文本键参数。
     * @param resultName 结果名称参数。
     * @return String 类型的处理结果。
     */
    private String readText(String resultPayload, String textKey, String resultName) {
        try {
            Map<String, Object> result = objectMapper.readValue(resultPayload, MAP_TYPE);
            Object text = result.get(textKey);
            return text == null ? "" : String.valueOf(text).trim();
        } catch (Exception exception) {
            log.warn("Failed to parse {}", resultName, exception);
            return "";
        }
    }

    /**
     * 加载模型。
     * @param environment 环境参数。
     * @return Model 类型的处理结果。
     */
    private Model loadModel(Environment environment) {
        String configuredPath = firstNonBlank(
                environment.getProperty("speech.vosk.model-path"),
                System.getenv("VOSK_MODEL_PATH"),
                DEFAULT_MODEL_PATH
        );
        Path modelPath = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(modelPath)) {
            log.warn("Vosk 模型路径不存在: {}", modelPath);
            return null;
        }

        if (hasNonAscii(modelPath.toString())) {
            log.info("Vosk 模型路径包含非 ASCII 字符，正在从 ASCII 临时路径加载");
            return loadFromAsciiTempPath(modelPath);
        }

        try {
            log.info("正在加载来自 {} 的 Vosk 模型", modelPath);
            return new Model(modelPath.toString());
        } catch (Exception exception) {
            log.warn("无法从 {} 加载 Vosk 模型，正在尝试 ASCII 临时路径", modelPath);
            log.debug("Vosk 模型加载失败", exception);
            return loadFromAsciiTempPath(modelPath);
        }
    }

    /**
     * 加载来源AsciiTemp路径。
     * @param sourcePath 来源路径参数。
     * @return Model 类型的处理结果。
     */
    private Model loadFromAsciiTempPath(Path sourcePath) {
        Path targetPath = Path.of(System.getProperty("java.io.tmpdir"), "smart-lab-vosk-model-cn-0.22").toAbsolutePath().normalize();
        try {
            copyDirectory(sourcePath, targetPath);
            log.info("从复制的路径 {} 加载 Vosk 模型", targetPath);
            return new Model(targetPath.toString());
        } catch (Exception exception) {
            log.warn("无法从复制的路径 {} 加载 Vosk 模型", targetPath, exception);
            return null;
        }
    }

    /**
     * 复制目录。
     * @param sourcePath 来源路径参数。
     * @param targetPath 目标路径参数。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private void copyDirectory(Path sourcePath, Path targetPath) throws Exception {
        if (Files.exists(targetPath.resolve("am").resolve("final.mdl"))) {
            return;
        }
        Files.createDirectories(targetPath);
        try (Stream<Path> stream = Files.walk(sourcePath)) {
            for (Path source : stream.toList()) {
                Path target = targetPath.resolve(sourcePath.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * first非空白的处理逻辑。
     * @return String 类型的处理结果。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return DEFAULT_MODEL_PATH;
    }

    /**
     * 判断是否具有非Ascii。
     * @param value 值参数。
     * @return 当前判断的布尔结果。
     */
    private boolean hasNonAscii(String value) {
        return value != null && !value.chars().allMatch(/** 处理 allMatch 调用中的回调逻辑。 入参：character。 */ character -> character < 128);
    }
}
