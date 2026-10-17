/*
 * 语音识别公共逻辑模块，供相关视图或服务复用。
 * 主要导出：BrowserSpeechRecognition。
 */
export class BrowserSpeechRecognition {
  /**
   * 初始化当前实例及其所需状态。
   * 入参：解构参数（onTranscript、onError）。
   */
  constructor({ onTranscript, onError } = {}) {
    this.onTranscript = onTranscript
    this.onError = onError
    this.recognition = null
    this.finalTranscript = ''
    this.interimTranscript = ''
    this.isListening = false
  }

  /**
   * 判断是否为Supported。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  get isSupported() {
    return Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  }

  /**
   * 启动当前操作。
   */
  start() {
    if (!this.isSupported) {
      throw new Error('当前浏览器不支持语音识别')
    }

    this.abort()
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    this.recognition = new SpeechRecognition()
    this.finalTranscript = ''
    this.interimTranscript = ''
    this.isListening = true

    this.recognition.lang = 'zh-CN'
    this.recognition.continuous = true
    this.recognition.interimResults = true
    this.recognition.maxAlternatives = 1

    this.recognition.onresult = /** 处理当前调用传入的回调逻辑。 入参：event。 */ (event) => {
      let interim = ''
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const text = event.results[index]?.[0]?.transcript?.trim() || ''
        if (!text) continue
        if (event.results[index].isFinal) {
          this.finalTranscript = `${this.finalTranscript} ${text}`.trim()
        } else {
          interim = `${interim} ${text}`.trim()
        }
      }
      this.interimTranscript = interim
      this.onTranscript?.(this.getTranscript())
    }

    this.recognition.onerror = /** 处理当前调用传入的回调逻辑。 入参：event。 */ (event) => {
      this.onError?.(event)
    }

    this.recognition.onend = /** 处理当前调用传入的回调逻辑。 */ () => {
      this.isListening = false
    }

    this.recognition.start()
  }

  /**
   * 停止当前操作。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  stop() {
    if (!this.recognition) {
      return Promise.resolve(this.getTranscript())
    }

    const recognition = this.recognition
    return new Promise(/** 处理当前调用传入的回调逻辑。 入参：resolve。 */ (resolve) => {
      let settled = false
      /**
       * 结束当前操作。
       */
      const finish = () => {
        if (settled) return
        settled = true
        this.recognition = null
        this.isListening = false
        resolve(this.getTranscript())
      }

      recognition.onend = finish
      window.setTimeout(finish, 900)

      try {
        recognition.stop()
      } catch {
        finish()
      }
    })
  }

  /**
   * abort的处理逻辑。
   */
  abort() {
    if (!this.recognition) return
    try {
      this.recognition.abort()
    } catch {
      // Ignore browser-specific abort timing errors.
    }
    this.recognition = null
    this.isListening = false
  }

  /**
   * 获取Transcript。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  getTranscript() {
    return (this.finalTranscript || this.interimTranscript || '').trim()
  }
}
