import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    root: '.',
    // Kapsam buyudukce paralel calismada bazi mount testleri 5sn'yi asiyor.
    testTimeout: 15000,
    hookTimeout: 15000,
    setupFiles: ['./src/test-setup.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/**/*.{js,vue}'],
      exclude: ['src/main.js', 'src/**/__tests__/**', 'src/locales/**'],
      thresholds: {
        statements: 16,
        lines: 16,
        functions: 8,
        branches: 6,
        'src/api/modules/**': {
          statements: 60,
          lines: 60,
          functions: 60,
          branches: 50
        }
      }
    },
    server: {
      deps: {
        inline: ['primevue', 'primeicons']
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
