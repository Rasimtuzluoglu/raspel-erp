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
import Skeleton from 'primevue/skeleton'

import AppDataTable from './components/AppDataTable.vue'
import PageHeader from './components/PageHeader.vue'
import EmptyState from './components/EmptyState.vue'
import SkeletonLoader from './components/SkeletonLoader.vue'

import 'primeicons/primeicons.css'
import './assets/app.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Lara,
    options: { darkModeSelector: false }
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
app.component('Skeleton', Skeleton)
app.component('AppDataTable', AppDataTable)
app.component('PageHeader', PageHeader)
app.component('EmptyState', EmptyState)
app.component('SkeletonLoader', SkeletonLoader)

app.mount('#app')
