/*
 * assistantAudio公共逻辑模块，供相关视图或服务复用。
 * 主要导出：AssistantAudio。
 */
export class AssistantAudio {
  /**
   * 初始化当前实例及其所需状态。
   * 入参：解构参数（onAudioFrame、onLevelChange）。
   */
  constructor({ onAudioFrame, onLevelChange } = {}) {
    this.onAudioFrame = onAudioFrame
    this.onLevelChange = onLevelChange
    this.audioContext = null
    this.mediaStream = null
    this.source = null
    this.processor = null
  }

  /**
   * 启动Capture。
   */
  async startCapture() {
    if (this.mediaStream) return

    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    })
    this.audioContext = new AudioContext()
    if (this.audioContext.state === 'suspended') {
      await this.audioContext.resume()
    }
    this.source = this.audioContext.createMediaStreamSource(this.mediaStream)
    this.processor = this.audioContext.createScriptProcessor(4096, 1, 1)
    this.processor.onaudioprocess = /** 处理当前调用传入的回调逻辑。 入参：event。 */ (event) => {
      const input = event.inputBuffer.getChannelData(0)
      this.onLevelChange?.(this.calculateLevel(input))
      const pcm16 = this.toPcm16(input, this.audioContext.sampleRate, 16000)
      if (pcm16.byteLength > 0) {
        this.onAudioFrame?.(pcm16.buffer)
      }
    }
    this.source.connect(this.processor)
    this.processor.connect(this.audioContext.destination)
  }

  /**
   * 停止Capture。
   */
  stopCapture() {
    if (this.processor) {
      this.processor.disconnect()
      this.processor.onaudioprocess = null
      this.processor = null
    }
    if (this.source) {
      this.source.disconnect()
      this.source = null
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(/** 处理遍历到的当前条目。 入参：track。 */ (track) => track.stop())
      this.mediaStream = null
    }
    if (this.audioContext) {
      this.audioContext.close()
      this.audioContext = null
    }
    this.onLevelChange?.(0)
  }

  /**
   * 销毁当前操作。
   */
  destroy() {
    this.stopCapture()
  }

  /**
   * 计算Level。
   * 入参：samples（samples）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  calculateLevel(samples) {
    let sum = 0
    for (let index = 0; index < samples.length; index += 1) {
      sum += samples[index] * samples[index]
    }
    return Math.sqrt(sum / samples.length)
  }

  /**
   * 转换为Pcm16。
   * 入参：samples（samples）；sourceRate（来源Rate）；targetRate（目标Rate）。
   * @returns pcm 的当前值。
   */
  toPcm16(samples, sourceRate, targetRate) {
    const ratio = sourceRate / targetRate
    const length = Math.floor(samples.length / ratio)
    const pcm = new Int16Array(length)
    for (let index = 0; index < length; index += 1) {
      const sourceIndex = Math.floor(index * ratio)
      const sample = Math.max(-1, Math.min(1, samples[sourceIndex] || 0))
      pcm[index] = sample < 0 ? sample * 0x8000 : sample * 0x7fff
    }
    return pcm
  }
}
