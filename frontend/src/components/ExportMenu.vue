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
import * as XLSX from 'xlsx'

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

const coz = () => {
  const cols =
    props.columns.length > 0 ? props.columns : Object.keys(props.data[0] || {}).map((k) => ({ field: k, header: k }))
  const satirlar = props.data.map((row) => cols.map((c) => {
    const v = row[c.field]
    return v == null ? '' : v
  }))
  return { cols, satirlar }
}

const exportCSV = () => {
  if (!props.data || !props.data.length) return
  const { cols, satirlar } = coz()
  const kacis = (v) => {
    const s = String(v ?? '')
    return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s
  }
  const header = cols.map((c) => kacis(c.header || c.field)).join(',')
  const rows = satirlar.map((r) => r.map(kacis).join(','))
  const csvContent = '\uFEFF' + [header, ...rows].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${props.filename}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

const exportExcel = () => {
  if (!props.data || !props.data.length) return
  const { cols, satirlar } = coz()
  const basliklar = cols.map((c) => c.header || c.field)
  const ws = XLSX.utils.aoa_to_sheet([basliklar, ...satirlar])
  ws['!cols'] = basliklar.map((b) => ({ wch: Math.min(40, Math.max(12, String(b).length + 4)) }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Veri')
  XLSX.writeFile(wb, `${props.filename}.xlsx`)
}
</script>
