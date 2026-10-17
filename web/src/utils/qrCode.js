/*
 * 二维码公共逻辑模块，供相关视图或服务复用。
 * 主要导出：generateQrSvg、generateQrSvgDataUrl。
 */
const VERSION_CONFIGS = [
  { version: 1, size: 21, dataCodewords: 19, ecCodewords: 7 },
  { version: 2, size: 25, dataCodewords: 34, ecCodewords: 10 },
  { version: 3, size: 29, dataCodewords: 55, ecCodewords: 15 },
  { version: 4, size: 33, dataCodewords: 80, ecCodewords: 20 },
  { version: 5, size: 37, dataCodewords: 108, ecCodewords: 26 },
]

const MODE_BYTE = 0b0100
const FORMAT_ERROR_CORRECTION_LOW = 0b01
const FORMAT_MASK_PATTERN = 0
const PAD_CODEWORDS = [0xec, 0x11]

const EXP_TABLE = new Array(512)
const LOG_TABLE = new Array(256)

let fieldValue = 1
for (let index = 0; index < 255; index += 1) {
  EXP_TABLE[index] = fieldValue
  LOG_TABLE[fieldValue] = index
  fieldValue <<= 1
  if (fieldValue & 0x100) {
    fieldValue ^= 0x11d
  }
}

for (let index = 255; index < EXP_TABLE.length; index += 1) {
  EXP_TABLE[index] = EXP_TABLE[index - 255]
}

const multiplyInField = (left, right) => {
  if (!left || !right) return 0
  return EXP_TABLE[LOG_TABLE[left] + LOG_TABLE[right]]
}

const appendBits = (target, value, length) => {
  for (let bit = length - 1; bit >= 0; bit -= 1) {
    target.push(((value >>> bit) & 1) === 1)
  }
}

const getPayloadBytes = (value) => Array.from(new TextEncoder().encode(String(value)))

const getQrConfig = (bytes) => {
  const dataBitLength = 4 + 8 + bytes.length * 8
  const config = VERSION_CONFIGS.find((item) => dataBitLength <= item.dataCodewords * 8)

  if (!config) {
    throw new Error('二维码内容过长，请缩短资产码链接')
  }

  return config
}

const createDataCodewords = (bytes, config) => {
  const bits = []
  const capacity = config.dataCodewords * 8

  appendBits(bits, MODE_BYTE, 4)
  appendBits(bits, bytes.length, 8)
  bytes.forEach((byte) => appendBits(bits, byte, 8))
  appendBits(bits, 0, Math.min(4, capacity - bits.length))

  while (bits.length % 8 !== 0) {
    bits.push(false)
  }

  const dataCodewords = []
  for (let index = 0; index < bits.length; index += 8) {
    let codeword = 0
    for (let offset = 0; offset < 8; offset += 1) {
      codeword = (codeword << 1) | (bits[index + offset] ? 1 : 0)
    }
    dataCodewords.push(codeword)
  }

  let padIndex = 0
  while (dataCodewords.length < config.dataCodewords) {
    dataCodewords.push(PAD_CODEWORDS[padIndex % PAD_CODEWORDS.length])
    padIndex += 1
  }

  return dataCodewords
}

const createErrorCorrectionDivisor = (degree) => {
  const result = new Array(degree).fill(0)
  result[degree - 1] = 1

  let root = 1
  for (let index = 0; index < degree; index += 1) {
    for (let cursor = 0; cursor < result.length; cursor += 1) {
      result[cursor] = multiplyInField(result[cursor], root)
      if (cursor + 1 < result.length) {
        result[cursor] ^= result[cursor + 1]
      }
    }
    root = multiplyInField(root, 2)
  }

  return result
}

const createErrorCorrectionCodewords = (dataCodewords, ecCodewordCount) => {
  const divisor = createErrorCorrectionDivisor(ecCodewordCount)
  const result = new Array(ecCodewordCount).fill(0)

  dataCodewords.forEach((codeword) => {
    const factor = codeword ^ result.shift()
    result.push(0)

    divisor.forEach((coefficient, index) => {
      result[index] ^= multiplyInField(coefficient, factor)
    })
  })

  return result
}

const getFormatBits = () => {
  const data = (FORMAT_ERROR_CORRECTION_LOW << 3) | FORMAT_MASK_PATTERN
  let remainder = data

  for (let index = 0; index < 10; index += 1) {
    remainder = (remainder << 1) ^ ((remainder >>> 9) * 0x537)
  }

  return ((data << 10) | (remainder & 0x3ff)) ^ 0x5412
}

const getFormatBit = (bits, index) => ((bits >>> index) & 1) === 1

const createMatrix = (config, codewords) => {
  const { size } = config
  const modules = Array.from({ length: size }, () => new Array(size).fill(false))
  const functionModules = Array.from({ length: size }, () => new Array(size).fill(false))

  const setFunctionModule = (x, y, isDark) => {
    if (x < 0 || y < 0 || x >= size || y >= size) return
    modules[y][x] = isDark
    functionModules[y][x] = true
  }

  const drawFinderPattern = (centerX, centerY) => {
    for (let y = -4; y <= 4; y += 1) {
      for (let x = -4; x <= 4; x += 1) {
        const distance = Math.max(Math.abs(x), Math.abs(y))
        setFunctionModule(centerX + x, centerY + y, distance !== 2 && distance !== 4)
      }
    }
  }

  const drawAlignmentPattern = (centerX, centerY) => {
    for (let y = -2; y <= 2; y += 1) {
      for (let x = -2; x <= 2; x += 1) {
        setFunctionModule(centerX + x, centerY + y, Math.max(Math.abs(x), Math.abs(y)) !== 1)
      }
    }
  }

  const drawFunctionPatterns = () => {
    drawFinderPattern(3, 3)
    drawFinderPattern(size - 4, 3)
    drawFinderPattern(3, size - 4)

    for (let index = 0; index < size; index += 1) {
      if (!functionModules[6][index]) setFunctionModule(index, 6, index % 2 === 0)
      if (!functionModules[index][6]) setFunctionModule(6, index, index % 2 === 0)
    }

    if (config.version >= 2) {
      const position = size - 7
      drawAlignmentPattern(position, position)
    }

    drawFormatBits()
  }

  const drawFormatBits = () => {
    const bits = getFormatBits()

    for (let index = 0; index <= 5; index += 1) setFunctionModule(8, index, getFormatBit(bits, index))
    setFunctionModule(8, 7, getFormatBit(bits, 6))
    setFunctionModule(8, 8, getFormatBit(bits, 7))
    setFunctionModule(7, 8, getFormatBit(bits, 8))
    for (let index = 9; index < 15; index += 1) setFunctionModule(14 - index, 8, getFormatBit(bits, index))

    for (let index = 0; index < 8; index += 1) setFunctionModule(size - 1 - index, 8, getFormatBit(bits, index))
    for (let index = 8; index < 15; index += 1) setFunctionModule(8, size - 15 + index, getFormatBit(bits, index))
    setFunctionModule(8, size - 8, true)
  }

  const isMaskDark = (x, y) => (x + y) % 2 === 0

  const drawCodewords = () => {
    let bitIndex = 0
    let upward = true
    const bitLength = codewords.length * 8

    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right -= 1

      for (let vertical = 0; vertical < size; vertical += 1) {
        const y = upward ? size - 1 - vertical : vertical

        for (let column = 0; column < 2; column += 1) {
          const x = right - column
          if (functionModules[y][x]) continue

          const bit =
            bitIndex < bitLength ? ((codewords[Math.floor(bitIndex / 8)] >>> (7 - (bitIndex % 8))) & 1) === 1 : false
          modules[y][x] = bit !== isMaskDark(x, y)
          bitIndex += 1
        }
      }

      upward = !upward
    }
  }

  drawFunctionPatterns()
  drawCodewords()
  drawFormatBits()

  return modules
}

export function generateQrSvg(value, options = {}) {
  const bytes = getPayloadBytes(value)
  const config = getQrConfig(bytes)
  const dataCodewords = createDataCodewords(bytes, config)
  const errorCorrectionCodewords = createErrorCorrectionCodewords(dataCodewords, config.ecCodewords)
  const matrix = createMatrix(config, [...dataCodewords, ...errorCorrectionCodewords])
  const border = options.border ?? 4
  const foreground = options.foreground || '#0f172a'
  const background = options.background || '#ffffff'
  const size = matrix.length + border * 2
  const paths = []

  matrix.forEach((row, y) => {
    row.forEach((isDark, x) => {
      if (isDark) {
        paths.push(`M${x + border} ${y + border}h1v1h-1z`)
      }
    })
  })

  return [
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size} ${size}" shape-rendering="crispEdges">`,
    `<path fill="${background}" d="M0 0h${size}v${size}H0z"/>`,
    `<path fill="${foreground}" d="${paths.join('')}"/>`,
    '</svg>',
  ].join('')
}

export function generateQrSvgDataUrl(value, options = {}) {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(generateQrSvg(value, options))}`
}
