import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    root: '.',
    setupFiles: ['./src/test-setup.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/**/*.{js,vue}'],
      exclude: ['src/main.js', 'src/**/__tests__/**', 'src/api/modules/**', 'src/locales/**']
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
