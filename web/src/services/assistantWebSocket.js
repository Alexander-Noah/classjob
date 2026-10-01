export class AssistantWebSocket {
  constructor({ onStatusChange, onTextMessage, onAudioMessage, onError } = {}) {
    this.ws = null
    this.sessionId = ''
    this.onStatusChange = onStatusChange
    this.onTextMessage = onTextMessage
    this.onAudioMessage = onAudioMessage
    this.onError = onError
  }

  get isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }

  connect(url, protocols = []) {
    this.disconnect()
    this.setStatus('connecting')
    this.ws = new WebSocket(url, protocols)
    this.ws.binaryType = 'blob'
    this.ws.onopen = () => {
      this.setStatus('connected')
      this.sendHello()
    }
    this.ws.onclose = () => {
      this.sessionId = ''
      this.setStatus('disconnected')
    }
    this.ws.onerror = (event) => {
      this.setStatus('error')
      this.onError?.(event)
    }
    this.ws.onmessage = async (event) => {
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

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  sendJson(message) {
    if (!this.isConnected) return false
    this.ws.send(JSON.stringify(message))
    return true
  }

  sendBinary(data) {
    if (!this.isConnected) return false
    this.ws.send(data)
    return true
  }

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

  sendText(text, images = []) {
    return this.sendJson({
      type: 'listen',
      state: 'detect',
      text,
      source: 'text',
      images,
    })
  }

  sendListenStart() {
    return this.sendJson({ type: 'listen', state: 'start', mode: 'auto' })
  }

  sendListenStop() {
    return this.sendJson({ type: 'listen', state: 'stop', mode: 'auto' })
  }

  sendAbort() {
    return this.sendJson({ type: 'abort', session_id: this.sessionId })
  }

  setStatus(status) {
    this.onStatusChange?.(status)
  }
}
