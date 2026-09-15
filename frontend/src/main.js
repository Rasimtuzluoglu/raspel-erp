import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Lara from '@primevue/themes/lara'
import App from './App.vue'
import router from './router/index.js'
import i18n from './i18n.js'

import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import AppDataTable from './components/AppDataTable.vue'
import PageHeader from './components/PageHeader.vue'
import EmptyState from './components/EmptyState.vue'
import SkeletonLoader from './components/SkeletonLoader.vue'
import ExportMenu from './components/ExportMenu.vue'
import GecmisZamanCizelgesi from './components/GecmisZamanCizelgesi.vue'
import SatirEylemleri from './components/SatirEylemleri.vue'

import permissionDirective from './directives/permission.js'
import tabloEtiketDirective, { initTabloEtiketleri } from './directives/tabloEtiket.js'

import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'
import './assets/tailwind.css'
import './assets/app.css'
import { useTheme } from './composables/useTheme.js'
import { formatCurrency, formatDate, formatDateTime } from './utils/format.js'
import { pvTr } from './utils/primevueLocales.js'

const { initTheme } = useTheme()
initTheme()

const app = createApp(App)

app.config.globalProperties.formatCurrency = formatCurrency
app.config.globalProperties.formatDate = formatDate
app.config.globalProperties.formatDateTime = formatDateTime

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Lara,
    options: { darkModeSelector: false }
  },
  locale: pvTr
})
app.use(ToastService)
app.use(ConfirmationService)
app.use(i18n)

app.component('AppDataTable', AppDataTable)
app.component('PageHeader', PageHeader)
app.component('EmptyState', EmptyState)
app.component('SkeletonLoader', SkeletonLoader)
app.component('ExportMenu', ExportMenu)
app.component('GecmisZamanCizelgesi', GecmisZamanCizelgesi)
app.component('SatirEylemleri', SatirEylemleri)

app.directive('permission', permissionDirective)
app.directive('tablo-etiket', tabloEtiketDirective)

app.mount('#app')

initTabloEtiketleri()
