/*
 * vite.config.js 构建配置，区分开发服务、编译输出与插件行为。
 */
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

const chunkLimitBytes = 450 * 1024
/**
 * nodeModule的处理逻辑。
 * 入参：name（名称）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const nodeModule = (name) => new RegExp(`node_modules[\\\\/]${name}[\\\\/]`)

const buildChunkGroups = [
  {
    name: 'echarts',
    test: /node_modules[\\/](echarts|zrender)[\\/]/,
    priority: 50,
    maxSize: chunkLimitBytes,
  },
  {
    name: 'element-plus',
    test: /node_modules[\\/](element-plus|@element-plus)[\\/]/,
    priority: 40,
    maxSize: chunkLimitBytes,
  },
  {
    name: 'vue-core',
    /**
     * 验证当前操作。
     * 入参：id（标识）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    test: (id) => nodeModule('@vue').test(id) || nodeModule('vue').test(id) || nodeModule('vue-router').test(id),
    priority: 35,
  },
  {
    name: 'management-modules',
    test: /src[\\/]constants[\\/]managementModules\.js$/,
    priority: 30,
    maxSize: chunkLimitBytes,
  },
  {
    name: 'statistics-page',
    test: /src[\\/]views[\\/]statistics[\\/]/,
    priority: 25,
    maxSize: chunkLimitBytes,
  },
  {
    name: 'app-api',
    test: /src[\\/]api[\\/]/,
    priority: 20,
    maxSize: chunkLimitBytes,
  },
  {
    name: 'vendor',
    test: /node_modules[\\/]/,
    priority: 10,
    maxSize: chunkLimitBytes,
  },
]

export default defineConfig(/** 处理 defineConfig 调用中的回调逻辑。 */ ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8080'
  const shouldProxyApi = !env.VITE_API_BASE_URL || env.VITE_API_BASE_URL === '/api'
  const devHost = env.VITE_DEV_HOST || '0.0.0.0'

  return {
    plugins: [vue()],
    build: {
      rolldownOptions: {
        output: {
          codeSplitting: {
            minSize: 0,
            maxSize: chunkLimitBytes,
            groups: buildChunkGroups,
          },
        },
      },
    },
    server: {
      host: devHost,
      proxy: shouldProxyApi
        ? {
            '/api': {
              target: proxyTarget,
              changeOrigin: true,
              secure: false,
              /**
               * rewrite的处理逻辑。
               * 入参：path（路径）。
               * @returns 由当前表达式或分支生成的处理结果。
               */
              rewrite: (path) => path.replace(/^\/api/, ''),
            },
          }
        : undefined,
    },
  }
})
