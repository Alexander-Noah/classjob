/*
 * 语音识别能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.vosk.Recognizer;

public interface SpeechRecognitionService {
    /**
     * 判断是否为可用。
     * @return 当前判断的布尔结果。
     */
    boolean isAvailable();

    /**
     * 创建Recognizer。
     * @return Recognizer 类型的处理结果。
     */
    Recognizer createRecognizer();

    /**
     * 获取结果文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    String getResultText(Recognizer recognizer);

    /**
     * 获取Partial文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    String getPartialText(Recognizer recognizer);

    /**
     * 获取Final文本。
     * @param recognizer recognizer参数。
     * @return String 类型的处理结果。
     */
    String getFinalText(Recognizer recognizer);
}
