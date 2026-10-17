/*
 * 语音识别能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.vosk.Recognizer;

public interface SpeechRecognitionService {
    boolean isAvailable();

    Recognizer createRecognizer();

    String getResultText(Recognizer recognizer);

    String getPartialText(Recognizer recognizer);

    String getFinalText(Recognizer recognizer);
}
