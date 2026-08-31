import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { PrimeVueResolver } from '@primevue/auto-import-resolver'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [
        PrimeVueResolver()
      ],
      directives: true
    }),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'script-defer',
      devOptions: { enabled: false },
      manifest: {
        name: 'RasPel ERP',
        short_name: 'RasPel',
        description: 'RasPel ERP — KOBİ yönetim sistemi',
        theme_color: '#0f172a',
        background_color: '#0f172a',
        lang: 'tr',
        categories: ['business', 'finance', 'productivity'],
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any'
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable'
          }
        ]
      },
      workbox: {
        navigateFallback: '/index.html',
        runtimeCaching: [
          {
            urlPattern: /\/api\//,
            method: 'GET',
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-read',
              networkTimeoutSeconds: 4,
              cacheableResponse: { statuses: [0, 200] },
              expiration: {
                maxEntries: 200,
                maxAgeSeconds: 60 * 60 * 24 * 7
              }
            }
          },
          {
            urlPattern: /\.(?:png|jpg|jpeg|svg|gif|ico|woff2?|ttf|eot)$/,
            handler: 'CacheFirst',
            options: {
              cacheName: 'static-assets',
              cacheableResponse: { statuses: [0, 200] },
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 * 365
              }
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.(googleapis|gstatic)\.com\//,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'google-fonts',
              cacheableResponse: { statuses: [0, 200] },
              expiration: {
                maxEntries: 20,
                maxAgeSeconds: 60 * 60 * 24 * 365
              }
            }
          }
        ]
      }
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
