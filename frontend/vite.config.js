import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: 'localhost',
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'primevue-core': ['primevue/button', 'primevue/datatable', 'primevue/column', 'primevue/dialog', 'primevue/inputtext', 'primevue/inputnumber', 'primevue/dropdown', 'primevue/card', 'primevue/toast', 'primevue/toastservice', 'primevue/confirmdialog', 'primevue/confirmationservice', 'primevue/toolbar', 'primevue/textarea', 'primevue/message', 'primevue/tag', 'primevue/tabview', 'primevue/tabpanel', 'primevue/inputswitch', 'primevue/skeleton', 'primevue/selectbutton', 'primevue/autocomplete', 'primevue/datepicker', 'primevue/calendar'],
          'primevue-icons': ['primeicons'],
          'vue-vendor': ['vue', 'vue-router', 'pinia', 'axios', 'axios-retry', 'vue-i18n'],
          'chart-vendor': ['chart.js', 'vue-chartjs']
        }
      }
    }
  }
})
