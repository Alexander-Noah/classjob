import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  plugins: [vue({ template: { transformAssetUrls: { includeAbsolute: false } } })],
  resolve: {
    alias: { 'node:test': fileURLToPath(new URL('./tests/support/nodeTestCompat.js', import.meta.url)) },
  },
  test: {
    environment: 'node',
    include: ['tests/**/*.test.{mjs,cjs,js}'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,ts,vue}'],
      reporter: ['text-summary', 'json', 'json-summary', 'html', 'lcov'],
      reportsDirectory: 'coverage',
      thresholds: { lines: 96 },
    },
  },
})
