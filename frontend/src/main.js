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

import permissionDirective from './directives/permission.js'

import 'primeicons/primeicons.css'
import './assets/app.css'
import { useTheme } from './composables/useTheme.js'
import { formatCurrency, formatDate, formatDateTime } from './utils/format.js'

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
  locale: {
    startsWith: 'Başlayan',
    contains: 'İçeren',
    notContains: 'İçermeyen',
    endsWith: 'Biten',
    equals: 'Eşit',
    notEquals: 'Eşit Değil',
    noFilter: 'Filtre Yok',
    accept: 'Evet',
    reject: 'Hayır',
    choose: 'Seç',
    upload: 'Yükle',
    cancel: 'İptal',
    dayNames: ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'],
    dayNamesShort: ['Pzr', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'],
    dayNamesMin: ['Pz', 'Pt', 'Sa', 'Çş', 'Pş', 'Cu', 'Ct'],
    monthNames: [
      'Ocak',
      'Şubat',
      'Mart',
      'Nisan',
      'Mayıs',
      'Haziran',
      'Temmuz',
      'Ağustos',
      'Eylül',
      'Ekim',
      'Kasım',
      'Aralık'
    ],
    monthNamesShort: ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'],
    today: 'Bugün',
    clear: 'Temizle',
    weekHeader: 'Hf',
    firstDayOfWeek: 1,
    dateFormat: 'dd.mm.yy'
  }
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

app.directive('permission', permissionDirective)

app.mount('#app')
