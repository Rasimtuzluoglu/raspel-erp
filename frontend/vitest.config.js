import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    root: '.',
    setupFiles: ['./src/test-setup.js'],
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
