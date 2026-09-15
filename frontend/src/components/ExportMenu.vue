<template>
  <div>
    <Button
      type="button"
      icon="pi pi-download"
      :label="$t('common.export')"
      class="p-button-outlined p-button-sm"
      aria-haspopup="true"
      aria-controls="export_menu"
      @click="toggleMenu"
    />
    <Menu
      id="export_menu"
      ref="menuRef"
      :model="items"
      :popup="true"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  data: { type: Array, default: () => [] },
  filename: { type: String, default: 'disa_aktarim' },
  columns: { type: Array, default: () => [] } // [{ field: 'ad', header: 'İsim' }]
})

const { t } = useI18n()
const menuRef = ref(null)

const items = computed(() => [
  {
    label: t('common.downloadExcel'),
    icon: 'pi pi-file-excel',
    command: () => exportExcel()
  },
  {
    label: t('common.downloadCsv'),
    icon: 'pi pi-file',
    command: () => exportCSV()
  },
  {
    label: t('common.print'),
    icon: 'pi pi-print',
    command: () => window.print()
  }
])

const toggleMenu = (event) => {
  menuRef.value.toggle(event)
}

const exportCSV = () => {
  if (!props.data || !props.data.length) return
  const cols =
    props.columns.length > 0 ? props.columns : Object.keys(props.data[0]).map((k) => ({ field: k, header: k }))
  const header = cols.map((c) => c.header).join(',')
  const rows = props.data.map((row) => cols.map((c) => row[c.field] || '').join(','))
  const csvContent = [header, ...rows].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${props.filename}.csv`
  link.click()
}

const exportExcel = async () => {
  if (!props.data || !props.data.length) return
  // Gelişmiş Excel kütüphanesi (Örn: xlsx) eklenirse buraya entegre edilir.
  // Şimdilik CSV olarak yönlendiriyoruz veya tablo HTML'ini basıyoruz.
  exportCSV()
}
</script>
