export class BrowserSpeechRecognition {
  constructor({ onTranscript, onError } = {}) {
    this.onTranscript = onTranscript
    this.onError = onError
    this.recognition = null
    this.finalTranscript = ''
    this.interimTranscript = ''
    this.isListening = false
  }

  get isSupported() {
    return Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  }

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

    this.recognition.onresult = (event) => {
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

    this.recognition.onerror = (event) => {
      this.onError?.(event)
    }

    this.recognition.onend = () => {
      this.isListening = false
    }

    this.recognition.start()
  }

  stop() {
    if (!this.recognition) {
      return Promise.resolve(this.getTranscript())
    }

    const recognition = this.recognition
    return new Promise((resolve) => {
      let settled = false
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

  getTranscript() {
    return (this.finalTranscript || this.interimTranscript || '').trim()
  }
}
