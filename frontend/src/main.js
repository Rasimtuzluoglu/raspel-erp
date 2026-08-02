import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Lara from '@primevue/themes/lara'
import App from './App.vue'
import router from './router/index.js'
import i18n from './i18n.js'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'
import Dropdown from 'primevue/dropdown'
import Select from 'primevue/select'
import Card from 'primevue/card'
import Toast from 'primevue/toast'
import ToastService from 'primevue/toastservice'
import ConfirmDialog from 'primevue/confirmdialog'
import ConfirmationService from 'primevue/confirmationservice'
import Toolbar from 'primevue/toolbar'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Calendar from 'primevue/calendar'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import InputSwitch from 'primevue/inputswitch'
import Checkbox from 'primevue/checkbox'
import SelectButton from 'primevue/selectbutton'
import Skeleton from 'primevue/skeleton'

import AppDataTable from './components/AppDataTable.vue'
import PageHeader from './components/PageHeader.vue'
import EmptyState from './components/EmptyState.vue'
import SkeletonLoader from './components/SkeletonLoader.vue'

import 'primeicons/primeicons.css'
import './assets/app.css'
import { useTheme } from './composables/useTheme.js'

const { initTheme } = useTheme()
initTheme()

if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

const app = createApp(App)

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
    monthNames: ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'],
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

app.component('DataTable', DataTable)
app.component('Column', Column)
app.component('Button', Button)
app.component('Dialog', Dialog)
app.component('InputText', InputText)
app.component('InputNumber', InputNumber)
app.component('DatePicker', DatePicker)
app.component('Dropdown', Dropdown)
app.component('Select', Select)
app.component('Card', Card)
app.component('Toast', Toast)
app.component('ConfirmDialog', ConfirmDialog)
app.component('Toolbar', Toolbar)
app.component('Textarea', Textarea)
app.component('Message', Message)
app.component('Tag', Tag)
app.component('Calendar', Calendar)
app.component('TabView', TabView)
app.component('TabPanel', TabPanel)
app.component('InputSwitch', InputSwitch)
app.component('Checkbox', Checkbox)
app.component('SelectButton', SelectButton)
app.component('Skeleton', Skeleton)
app.component('AppDataTable', AppDataTable)
app.component('PageHeader', PageHeader)
app.component('EmptyState', EmptyState)
app.component('SkeletonLoader', SkeletonLoader)

app.mount('#app')
