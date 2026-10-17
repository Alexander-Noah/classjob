/*
 * assistantWebSocket公共逻辑模块，供相关视图或服务复用。
 * 主要导出：AssistantWebSocket。
 */
/**
 * AI 助手文本/音频通道，维护单个 WebSocket 实例及服务端 hello 返回的会话 ID。
 * 不内置重连、离线队列或发送重试；组件负责连接时机、错误提示和卸载时 disconnect。
 */
export class AssistantWebSocket {
  /**
   * 初始化当前实例及其所需状态。
   * 入参：解构参数（onStatusChange、onTextMessage、onAudioMessage、onError）。
   */
  constructor({ onStatusChange, onTextMessage, onAudioMessage, onError } = {}) {
    this.ws = null
    this.sessionId = ''
    this.onStatusChange = onStatusChange
    this.onTextMessage = onTextMessage
    this.onAudioMessage = onAudioMessage
    this.onError = onError
  }

  /**
   * 判断是否为Connected。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  get isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }

  /**
   * 关闭旧连接后建立新连接；连接成功时自动发送 v3 hello。
   * @param {string} url 已构造好的 ws/wss 地址，禁止记录包含令牌的完整地址
   * @param {Array<string>} protocols 浏览器 WebSocket 子协议，不是 HTTP 请求头
   * @returns {void} 连接异步完成，通过 onStatusChange/onError 回调通知调用方
   * @throws {DOMException} 浏览器拒绝非法 URL、协议或安全上下文时可能同步抛出
   */
  connect(url, protocols = []) {
    this.disconnect()
    this.setStatus('connecting')
    this.ws = new WebSocket(url, protocols)
    this.ws.binaryType = 'blob'
    this.ws.onopen = /** 处理当前调用传入的回调逻辑。 */ () => {
      this.setStatus('connected')
      this.sendHello()
    }
    this.ws.onclose = /** 处理当前调用传入的回调逻辑。 */ () => {
      this.sessionId = ''
      this.setStatus('disconnected')
    }
    this.ws.onerror = /** 处理当前调用传入的回调逻辑。 入参：event。 */ (event) => {
      this.setStatus('error')
      this.onError?.(event)
    }
    this.ws.onmessage = /** 处理当前调用传入的回调逻辑。 入参：event。 */ async (event) => {
      // 二进制帧交给音频回调；文本帧按 JSON 解析，非 JSON 文本仍交由文本回调展示。
      if (event.data instanceof Blob) {
        this.onAudioMessage?.(await event.data.arrayBuffer())
        return
      }

      try {
        const message = JSON.parse(event.data)
        if (message.type === 'hello' && message.session_id) {
          this.sessionId = message.session_id
        }
        this.onTextMessage?.(message)
      } catch {
        this.onTextMessage?.({ type: 'text', text: String(event.data || '') })
      }
    }
  }

  /**
   * 断开当前操作。
   */
  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  /** 返回值只表示当前连接允许提交帧，不代表服务端已确认业务处理完成。 */
  sendJson(message) {
    if (!this.isConnected) return false
    this.ws.send(JSON.stringify(message))
    return true
  }

  /**
   * 发送Binary。
   * 入参：data（数据）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  sendBinary(data) {
    if (!this.isConnected) return false
    this.ws.send(data)
    return true
  }

  // 协议约定：PCM signed 16-bit little-endian，16 kHz，单声道；服务端须使用相同采样参数。
  sendHello() {
    return this.sendJson({
      type: 'hello',
      version: 3,
      audio_params: {
        format: 'pcm_s16le',
        sample_rate: 16000,
        channels: 1,
      },
    })
  }

  /**
   * 发送文本。
   * 入参：text（文本）；images（images）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  sendText(text, images = []) {
    return this.sendJson({
      type: 'listen',
      state: 'detect',
      text,
      source: 'text',
      images,
    })
  }

  /**
   * 发送Listen开始。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  sendListenStart() {
    return this.sendJson({ type: 'listen', state: 'start', mode: 'auto' })
  }

  /**
   * 发送ListenStop。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  sendListenStop() {
    return this.sendJson({ type: 'listen', state: 'stop', mode: 'auto' })
  }

  /**
   * 发送Abort。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  sendAbort() {
    return this.sendJson({ type: 'abort', session_id: this.sessionId })
  }

  /**
   * 设置状态。
   * 入参：status（状态）。
   */
  setStatus(status) {
    this.onStatusChange?.(status)
  }
}
