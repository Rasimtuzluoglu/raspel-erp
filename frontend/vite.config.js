import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { PrimeVueResolver } from '@primevue/auto-import-resolver'
import { VitePWA } from 'vite-plugin-pwa'
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig(({ mode }) => ({
  define: {
    // Surum bilgisi build-time gomulur; eski onbellek tespitinde kullanilir.
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '0.0.0')
  },
  plugins: [
    vue(),
    Components({
      resolvers: [
        PrimeVueResolver()
      ],
      directives: true
    }),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.js',
      registerType: 'autoUpdate',
      injectRegister: false,
      devOptions: { enabled: false },
      // Yalnizca uygulama kabugu (index.html, manifest, ikonlar) precache edilir.
      // Route/vendor JS+CSS parcalari precache disi kalir; ilk ziyarette runtime
      // "static-assets" (CacheFirst) rotasi tarafindan onbellege alinir. Boylece
      // precache boyutu ~4.5MB'den birkac KB'ye iner ve lazy chunk'lar gercekten
      // talep uzerine yuklenir.
      injectManifest: {
        globPatterns: ['**/*.{html,ico,png,svg,webmanifest}'],
        globIgnores: ['**/assets/**']
      },
      manifest: {
        id: '/',
        name: 'RasPel ERP',
        short_name: 'RasPel',
        description: 'RasPel ERP — KOBİ yönetim sistemi',
        theme_color: '#0b0f14',
        background_color: '#0b0f14',
        lang: 'tr',
        dir: 'ltr',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        display_override: ['standalone', 'minimal-ui'],
        orientation: 'any',
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
      }
    }),
    // Yalnizca `--mode analyze` ile bundle analiz raporu uretir (uretim paketine girmez)
    mode === 'analyze' &&
      visualizer({
        filename: 'dist/stats.html',
        gzipSize: true,
        brotliSize: true,
        template: 'treemap',
        open: false
      })
  ].filter(Boolean),
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
          if (!id.includes('node_modules')) return
          // NOT: PrimeVue tek parca halinde zorlanmaz; PrimeVueResolver bilesenleri
          // kullanan gorunume gore otomatik import ettigi icin Rollup bunlari route
          // bazli boler. Boylece ~1MB'lik tekil primevue chunk'i ilk yukten kalkar.
          if (id.includes('node_modules/primeicons')) {
            return 'primevue-icons'
          }
          // Grafikler yalnizca Dashboard/Muhasebe/StokDetay gibi tembel
          // gorunumlerde kullanilir; kendi chunk'inda kalsin ki baslangicta
          // yuklenmesin. vue-chartjs de buraya dahil (aksi halde vendor
          // chunk'ina girip chart.js'i baslangica surukler).
          if (/[\\/]node_modules[\\/](chart\.js|vue-chartjs)[\\/]/.test(id)) {
            return 'chart-vendor'
          }
          // Cekirdek kutuphane: yalnizca tam paket adi eslesir.
          // (Onceden `node_modules/vue` gecen `vue-chartjs`i de yakaliyor ve
          //  Chart.js'i baslangic yukune sokuyordu.)
          if (/[\\/]node_modules[\\/](vue|vue-router|vue-i18n|pinia|axios|@vue)[\\/]/.test(id)) {
            return 'vue-vendor'
          }
        }
      }
    }
  }
}))
