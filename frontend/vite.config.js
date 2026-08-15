import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { PrimeVueResolver } from '@primevue/auto-import-resolver'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [
        PrimeVueResolver()
      ],
      directives: true
    })
  ],
  server: {
    port: 5173,
    host: 'localhost',
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8081',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/primevue') || id.includes('node_modules/@primevue')) {
            return 'primevue'
          }
          if (id.includes('primeicons')) {
            return 'primevue-icons'
          }
          if (id.includes('chart.js') || id.includes('vue-chartjs')) {
            return 'chart-vendor'
          }
          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia') || id.includes('node_modules/axios') || id.includes('node_modules/vue-router') || id.includes('node_modules/vue-i18n')) {
            return 'vue-vendor'
          }
        }
      }
    }
  }
})
